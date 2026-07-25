package org.entermediadb.ai.skills;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.Searcher;

public class AdaptiveTutorialProgressSkill extends BaseSkill
{
	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;

		String userid = messageContext.getUserProfile().getUser().getId();

		// String channelid = (String) messageContext.getContextValue("channelid");
		String sectionid = (String) messageContext.getContextValue("sectionid");

		Data section = getMediaArchive().getData("componentsection", sectionid);

		String tutorialid = section.get("playbackentityid");

		Collection<Data> allsections = getMediaArchive().query("componentsection").exact("playbackentitymoduleid", "entitytutorial").exact("playbackentityid", tutorialid).search();

		Collection<String> sectionids = allsections.stream().map(s -> s.getId()).toList();

		Collection<Data> allcomponentwithquestions = getMediaArchive().query("componentcontent").exact("componenttype", "mcq").orgroup("componentsectionid", sectionids).search();

		Collection<String> questionids = allcomponentwithquestions.stream().map(a -> a.get("questionid")).distinct().toList();
		Collection<Data> allquestions = getMediaArchive().query("entityquestion").ids(questionids).search();
		Collection<Data> allanswers = getMediaArchive().query("tutoranswer").orgroup("entityquestion", questionids).exact("user", userid).search();

		Collection<Map<String, Object>> combined = allquestions.stream().map(q -> {
			Map<String, Object> map = new HashMap<>();
			map.put("question", q);
			Data answer = allanswers.stream().filter(a -> a.get("entityquestion").equals(q.getId())).findFirst().orElse(null);
			map.put("answer", answer);
			return map;
		}).toList();

		Collection<MultiValued> mcqcognitivelevels = getMediaArchive().query("mcqcognitivelevel").all().search();
		Map<String, Double> cognitivelevelweights = new HashMap<>();
		for (MultiValued level : mcqcognitivelevels)
		{
			cognitivelevelweights.put(level.getId(), level.getDouble("weight"));
		}

		Collection<MultiValued> answerconfidences = getMediaArchive().query("answerconfidence").all().search();
		Map<String, Double> answerconfidenceweights = new HashMap<>();
		for (MultiValued confidence : answerconfidences)
		{
			answerconfidenceweights.put(confidence.getId(), confidence.getDouble("weight"));
		}

		double beginnerpoints = 0;
		double competentpoints = 0;
		double expertpoints = 0;

		double max_beginnerpoints = cognitivelevelweights.getOrDefault("beginner", 0.0) * answerconfidenceweights.getOrDefault("confident", 0.0);
		double max_competentpoints = cognitivelevelweights.getOrDefault("competent", 0.0) * answerconfidenceweights.getOrDefault("confident", 0.0);
		double max_expertpoints = cognitivelevelweights.getOrDefault("expert", 0.0) * answerconfidenceweights.getOrDefault("confident", 0.0);

		double total_beginnerpoints = 0;
		double total_competentpoints = 0;
		double total_expertpoints = 0;

		for (Map<String, Object> map : combined)
		{
			Data question = (Data) map.get("question");
			String cognitivelevel = question.get("mcqcognitivelevel");

			if ("beginner".equals(cognitivelevel))
			{
				total_beginnerpoints += max_beginnerpoints;
			}
			else if ("competent".equals(cognitivelevel))
			{
				total_competentpoints += max_competentpoints;
			}
			else if ("expert".equals(cognitivelevel))
			{
				total_expertpoints += max_expertpoints;
			}

			Data answer = (Data) map.get("answer");
			if (answer == null)
			{
				continue;
			}

			boolean correct = answer.get("selectedoption").equals(question.get("correctoption"));
			String answerconfidence = answer.get("answerconfidence");

			double cognitivelevelweight = cognitivelevelweights.getOrDefault(cognitivelevel, 0.0);
			double answerconfidenceweight = answerconfidenceweights.getOrDefault(answerconfidence, 0.0);

			double points = cognitivelevelweight * (answerconfidenceweight * (correct ? 1 : -1));

			double bonus = 0;
			if (correct && "confident".equals(answerconfidence))
			{
				bonus = answerconfidenceweight * 0.5;
			}
			else if (correct && "noidea".equals(answerconfidence))
			{
				bonus = answerconfidenceweight * -0.25;
			}
			else if (!correct && "confident".equals(answerconfidence))
			{
				bonus = answerconfidenceweight * -0.5;
			}
			else if (!correct && "noidea".equals(answerconfidence))
			{
				bonus = answerconfidenceweight * 0.5;
			}

			points += bonus;

			if ("beginner".equals(cognitivelevel))
			{
				beginnerpoints += points;
			}
			else if ("competent".equals(cognitivelevel))
			{
				competentpoints += points;
			}
			else if ("expert".equals(cognitivelevel))
			{
				expertpoints += points;
			}
		}

		double average_beginnerpoints = total_beginnerpoints > 0 ? beginnerpoints / total_beginnerpoints : 0;
		double average_competentpoints = total_competentpoints > 0 ? competentpoints / total_competentpoints : 0;
		double average_expertpoints = total_expertpoints > 0 ? expertpoints / total_expertpoints : 0;

		Searcher progresssearcher = getMediaArchive().getSearcher("tutorialprogress");

		Data progress = progresssearcher.query().exact("user", userid).exact("entitytutorial", tutorialid).searchOne();
		if (progress == null)
		{
			progress = progresssearcher.createNewData();
			progress.setProperty("user", userid);
			progress.setProperty("entitytutorial", tutorialid);
		}
		progress.setValue("beginnerprogress", average_beginnerpoints);
		progress.setValue("competentprogress", average_competentpoints);
		progress.setValue("expertprogress", average_expertpoints);
		progress.setValue("lastreviewed", new Date());
		progresssearcher.saveData(progress);

		Data agentmessage = messageContext.getAgentMessage();
		agentmessage.setValue("id", tutorialid + "_progressupdate");
		agentmessage.setValue("messagetype", "system");

		Map<String, Object> broadcastpayload = new HashMap<String, Object>();
		broadcastpayload.put("messageid", tutorialid + "_progressupdate");
		broadcastpayload.put("messagetype", "progressupdate");
		broadcastpayload.put("tutorialid", tutorialid);
		broadcastpayload.put("beginnerprogress", average_beginnerpoints);
		broadcastpayload.put("competentprogress", average_competentpoints);
		broadcastpayload.put("expertprogress", average_expertpoints);

		agentmessage.setValue("message", "Progress updated for tutorial " + tutorialid);

		messageContext.setValue("broadcastpayload", broadcastpayload);

		AgentEnabled skillEnabled = messageContext.getCurrentAgentEnable();
		messageContext.fireStatusComplete(skillEnabled);

	}
}
