package org.entermediadb.ai.skills;

import java.util.Date;
import java.util.Map;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.TutorMessageContext;
import org.entermediadb.ai.automation.RunningScenario;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.data.Searcher;

public class AdaptiveTutorialAnswerSkill extends AdaptiveTutorialBaseSkill
{
	@Override
	public void process(AgentContext inAgentContext)
	{
		TutorMessageContext tutorMessageContext = (TutorMessageContext) inAgentContext;

		String channelid = tutorMessageContext.getChannel().getId();
		String questionid = (String) tutorMessageContext.getContextValue("questionid");
		String confidence = (String) tutorMessageContext.getContextValue("confidence");
		String selectedoption = (String) tutorMessageContext.getContextValue("selectedoption");

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
		answer.setValue("user", tutorMessageContext.getUserProfile().getUser().getId());
		answer.setValue("datecreated", new Date());
		answer.setValue("lastpenalty", new Date());

		searcher.saveData(answer);

		tutorMessageContext.putContextValue("iscorrect", iscorrect);
		tutorMessageContext.putContextValue("question", question);
		tutorMessageContext.putContextValue("confidence", confidence);
		tutorMessageContext.putContextValue("selectedoption", selectedoption);

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("thinking");
		LlmResponse response = llmconnection.callStructure(tutorMessageContext, "chat_tutor_feedback");

		JSONObject feedback = response.getMessageStructured();
		String feedbackText = (String) feedback.get("message");

		tutorMessageContext.setLastResponse(response);
		tutorMessageContext.log("sent" + response.getMessagePlain());

		tutorMessageContext.setMessageAgentContext("sectionid", tutorMessageContext.getMessageAgentContext("sectionid"));
		tutorMessageContext.setMessageAgentContext("componentid", tutorMessageContext.getMessageAgentContext("componentid"));

		tutorMessageContext.setMessageAgentContext("componentcontent", feedbackText);
		tutorMessageContext.setMessageAgentContext("selectedoption", selectedoption);
		tutorMessageContext.setMessageAgentContext("confidence", confidence);
		tutorMessageContext.setMessageAgentContext("answerid", answer.getId());
		tutorMessageContext.setMessageAgentContext("messagetype", "answereval");
		tutorMessageContext.setMessageAgentContext("iscorrect", iscorrect);

		AgentEnabled skillEnabled = tutorMessageContext.getCurrentAgentEnable();
		tutorMessageContext.fireStatusComplete(skillEnabled);

		Data agentmessage = tutorMessageContext.getAgentMessage();

		agentmessage.setValue("id", tutorMessageContext.getTutorialId() + "_progressupdate");
		agentmessage.setValue("messagetype", "system");

		RunningScenario scenario = tutorMessageContext.getCurrentScenario();

		AgentEnabled nextAgentEnabled = scenario.findEnabled("chat_tutor_progress");
		TutorMessageContext nextContext = (TutorMessageContext) scenario.createAgentContext(tutorMessageContext, nextAgentEnabled);

		scenario.runProcess(nextAgentEnabled, nextContext, true);
	}
}
