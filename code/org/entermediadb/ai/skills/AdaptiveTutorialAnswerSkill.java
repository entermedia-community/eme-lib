package org.entermediadb.ai.skills;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.automation.RunningScenario;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.Data;
import org.openedit.data.Searcher;

public class AdaptiveTutorialAnswerSkill extends BaseSkill
{
	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;

		String channelid = (String) messageContext.getContextValue("channelid");
		String questionid = (String) messageContext.getContextValue("questionid");
		String confidence = (String) messageContext.getContextValue("confidence");
		String selectedoption = (String) messageContext.getContextValue("selectedoption");
		String sectionid = (String) messageContext.getContextValue("sectionid");
		Data section = getMediaArchive().getData("componentsection", sectionid);
		String componentid = (String) messageContext.getContextValue("componentid");

		if (channelid == null || questionid == null || selectedoption == null)
		{
			return;
		}

		Searcher searcher = getMediaArchive().getSearcher("tutoranswer");

		Data answer = searcher.createNewData();
		answer.setValue("channel", channelid);
		answer.setValue("entityquestion", questionid);
		answer.setValue("answerconfidence", confidence);
		answer.setValue("selectedoption", selectedoption);
		answer.setValue("user", messageContext.getUserProfile().getUser().getId());
		answer.setValue("datecreated", new Date());

		searcher.saveData(answer);

		// TODO: recalculate progress

		Data question = getMediaArchive().getData("entityquestion", questionid);
		if (question != null)
		{
			messageContext.putContextValue("question", question);
			messageContext.putContextValue("answer", answer);

			LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
			LlmResponse response = llmconnection.renderLocalAction(messageContext, "chat_tutor_answer");

			messageContext.setLastResponse(response);
			messageContext.log("sent" + response.getMessagePlain());

			Map<String, String> broadcastpayload = new HashMap<String, String>();
			// broadcastpayload.put("messageid", answer.getId());
			broadcastpayload.put("sectionid", sectionid);
			broadcastpayload.put("componentid", componentid);

			messageContext.setValue("broadcastpayload", broadcastpayload);

			AgentEnabled skillEnabled = messageContext.getCurrentAgentEnable();
			messageContext.fireStatusComplete(skillEnabled);
		}
		else
		{
			endTutorial(messageContext);
		}

		Data agentmessage = messageContext.getAgentMessage();
		agentmessage.setValue("id", section.get("playbackentityid") + "_progressupdate");
		agentmessage.setValue("messagetype", "system");

		Map<String, String> broadcastpayload = new HashMap<String, String>();
		broadcastpayload.put("messageid", section.get("playbackentityid") + "_progressupdate");
		messageContext.setValue("broadcastpayload", broadcastpayload);

		RunningScenario scenario = messageContext.getCurrentScenario();

		AgentEnabled nextAgentEnabled = scenario.findEnabled("chat_tutor_progress");
		AgentContext nextContext = scenario.createAgentContext(messageContext, nextAgentEnabled);
		scenario.runProcess(nextAgentEnabled, nextContext);
	}

	public void endTutorial(ChatMessageContext messageContext)
	{
		AgentEnabled currentAgentEnabled = messageContext.getCurrentScenario().findEnabled("chat_tutor_end");

		messageContext.setCurrentAgentEnable(currentAgentEnabled);
		messageContext.fireStatusComplete(currentAgentEnabled);
	}
}
