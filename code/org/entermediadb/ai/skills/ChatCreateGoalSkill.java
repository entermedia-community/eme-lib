package org.entermediadb.ai.skills;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.ai.llm.BasicLlmResponse;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.tasks.GoalManager;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;

public class ChatCreateGoalSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(ChatCreateGoalSkill.class);

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;
		MultiValued agentmessage = messageContext.getAgentMessage();

		MultiValued usermessage = (MultiValued) getMediaArchive().getCachedData("chatterbox", agentmessage.get("replytoid"));

		String userid = usermessage.get("user");
		String collectionid = inAgentContext.get("entityid"); // EmeChatDetectSkill sets the right collectionid in entityid
		JSONObject arguments = (JSONObject) inAgentContext.getContextValue("arguments");
		String taskdescription = (String) arguments.get("task");
		agentmessage.setValue("message", taskdescription);
		agentmessage.setValue("useralias", userid);
		getMediaArchive().saveData("chatterbox", agentmessage);

		Data goal = getGoalManager().createGoal(userid, agentmessage, collectionid);

		messageContext.putContextValue("goal", goal);

		/*
		 * LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender"); LlmResponse
		 * response = llmconnection.renderLocalAction(inAgentContext, "chat_goal_created");
		 */
		BasicLlmResponse response = new BasicLlmResponse();
		response.setNextSkillEnabled("emechat_responder_welcome");
		messageContext.setLastResponse(response);

		AgentEnabled skillEnabled = messageContext.getCurrentAgentEnable();
		messageContext.fireStatusComplete(skillEnabled);

		getMediaArchive().fireSharedMediaEvent("goaltask/goalcreated");

		return;
	}

	private GoalManager getGoalManager()
	{
		GoalManager goalm = (GoalManager) getMediaArchive().getBean("goalManager");
		return goalm;
	}

}
