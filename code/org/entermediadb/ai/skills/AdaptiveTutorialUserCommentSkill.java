package org.entermediadb.ai.skills;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.TutorMessageContext;
import org.entermediadb.ai.assistant.AssistantManager;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.users.User;

public class AdaptiveTutorialUserCommentSkill extends AdaptiveTutorialBaseSkill
{
	private static final Log log = LogFactory.getLog(AdaptiveTutorialUserCommentSkill.class);

	@Override
	public void process(AgentContext inAgentContext)
	{
		TutorMessageContext tutorMessageContext = (TutorMessageContext) inAgentContext;

		String channelid = tutorMessageContext.getChannel().getId();
		String tutorialid = (String) tutorMessageContext.getTutorialId();
		String sectionid = (String) tutorMessageContext.getMessageAgentContext("sectionid");
		String componentid = (String) tutorMessageContext.getMessageAgentContext("componentid");
		String query = (String) tutorMessageContext.getContextValue("query");
		String userId = tutorMessageContext.getUserProfile().getUser().getId();

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("embedding");

		JSONArray chatHistory = getReleaventChatHistory(channelid, userId);

		Collection<String> parentIds = getAssistantManager().findDocIdsForEntity("entitytutorial", tutorialid);

		if (parentIds.isEmpty())
		{
			log.info("No parent ids found!");
			return;
		}

		Map payload = new HashMap();
		payload.put("query", query);
		payload.put("parent_ids", parentIds);
		payload.put("chat_history", chatHistory);

		log.info("Sending /chat to embedding server with query: " + query + ", parent_ids: " + parentIds.size() + ", history size: " + chatHistory.size());

		LlmResponse res = llmconnection.callJson("/chat", payload);

		JSONObject contentsJson = res.getRawResponse();

		String answer = (String) contentsJson.get("answer");
		if (answer == null)
		{
			tutorMessageContext.error("No answer found " + answer);
			return;
		}

		Data existingMessage = tutorMessageContext.getAgentMessage();
		existingMessage.setValue("user", userId);
		tutorMessageContext.setMessageAgentContext("componentcontent", query);
		tutorMessageContext.setMessageAgentContext("messagetype", "usercomment");
		tutorMessageContext.setMessageAgentContext("tutorialid", tutorialid);
		tutorMessageContext.setMessageAgentContext("sectionid", sectionid);
		tutorMessageContext.setMessageAgentContext("componentid", componentid);
		getMediaArchive().saveData("chatterbox", existingMessage);

		MultiValued newMessage = (MultiValued) getMediaArchive().getSearcher("chatterbox").createNewData();
		newMessage.setValue("date", new Date());
		newMessage.setValue("channel", tutorMessageContext.getChannel().getId());
		newMessage.setValue("user", "agent");

		tutorMessageContext.setAgentMessage(newMessage);
		tutorMessageContext.setMessageAgentContext("componentcontent", answer);
		tutorMessageContext.setMessageAgentContext("messagetype", "text");
		tutorMessageContext.setMessageAgentContext("sectionid", sectionid);
		tutorMessageContext.setMessageAgentContext("componentid", componentid);
		tutorMessageContext.setMessageAgentContext("tutorialid", tutorialid);

		AgentEnabled skillEnabled = tutorMessageContext.getCurrentAgentEnable();
		tutorMessageContext.fireStatusComplete(skillEnabled);
	}
}
