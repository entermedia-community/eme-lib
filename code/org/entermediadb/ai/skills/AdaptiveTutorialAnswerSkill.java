package org.entermediadb.ai.skills;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.TutorMessageContext;
import org.entermediadb.ai.automation.RunningScenario;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.Data;
import org.openedit.data.Searcher;

public class AdaptiveTutorialAnswerSkill extends AdaptiveTutorialBaseSkill
{
	@Override
	public void process(AgentContext inAgentContext)
	{
		TutorMessageContext messageContext = (TutorMessageContext) inAgentContext;

		String channelid = (String) messageContext.getChannel().getId();
		String questionid = (String) messageContext.getContextValue("questionid");
		String confidence = (String) messageContext.getContextValue("confidence");
		String selectedoption = (String) messageContext.getContextValue("selectedoption");

		if (channelid == null || questionid == null || selectedoption == null)
		{
			return;
		}

		Data question = getMediaArchive().getData("entityquestion", questionid);
		if (question == null)
		{
			return;
		}

		boolean iscorrect = selectedoption.equals(question.get("correctoption"));

		Map<String, Double> cognitivelevelpoints = getCognitiveLevelPoints();
		Map<String, Double> answerconfidencebonus = getAnswerConfidenceBonus();

		double allottedpoints = cognitivelevelpoints.getOrDefault(question.get("mcqcognitivelevel"), 0.0);

		double points = 0.0;
		if (iscorrect)
		{
			points = allottedpoints;
		}

		double bonus = allottedpoints * (answerconfidencebonus.getOrDefault(confidence, 0.0) / 100.0);

		Searcher searcher = getMediaArchive().getSearcher("tutoranswer");

		Data answer = searcher.createNewData();
		answer.setValue("channel", channelid);
		answer.setValue("entityquestion", questionid);
		answer.setValue("answerconfidence", confidence);
		answer.setValue("selectedoption", selectedoption);
		answer.setValue("iscorrect", iscorrect);
		answer.setValue("pointsearned", points);
		answer.setValue("bonusearned", bonus);
		answer.setValue("user", messageContext.getUserProfile().getUser().getId());
		answer.setValue("datecreated", new Date());

		searcher.saveData(answer);

		messageContext.putContextValue("iscorrect", iscorrect);
		messageContext.putContextValue("correctoptiontext", question.get(question.get("correctoption")));
		messageContext.putContextValue("confidence", confidence);

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
		LlmResponse response = llmconnection.renderLocalAction(messageContext, "chat_tutor_answer");

		messageContext.setLastResponse(response);
		messageContext.log("sent" + response.getMessagePlain());

		Map<String, String> broadcastpayload = new HashMap<String, String>();
		broadcastpayload.put("sectionid", messageContext.getLastSectionId());
		broadcastpayload.put("componentid", messageContext.getLastComponentId());

		messageContext.setValue("broadcastpayload", broadcastpayload);

		AgentEnabled skillEnabled = messageContext.getCurrentAgentEnable();
		messageContext.fireStatusComplete(skillEnabled);

		Data agentmessage = messageContext.getAgentMessage();

		agentmessage.setValue("id", messageContext.getTutorialId() + "_progressupdate");
		agentmessage.setValue("messagetype", "system");

		RunningScenario scenario = messageContext.getCurrentScenario();

		AgentEnabled nextAgentEnabled = scenario.findEnabled("chat_tutor_progress");
		TutorMessageContext nextContext = (TutorMessageContext) scenario.createAgentContext(messageContext, nextAgentEnabled);

		scenario.runProcess(nextAgentEnabled, nextContext, true);
	}
}
