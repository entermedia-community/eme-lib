package org.entermediadb.ai.skills;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.TutorMessageContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.Searcher;

public class AdaptiveTutorialProgressSkill extends AdaptiveTutorialBaseSkill
{
	@Override
	public void process(AgentContext inAgentContext)
	{
		TutorMessageContext messageContext = (TutorMessageContext) inAgentContext;

		String userid = messageContext.getUserProfile().getUser().getId();
		String tutorialid = messageContext.getTutorialId();
		String channelid = messageContext.getChannel().getId();

		if (tutorialid == null || userid == null || channelid == null)
		{
			return;
		}

		Collection<Data> allsections = getMediaArchive().query("componentsection").exact("playbackentitymoduleid", "entitytutorial").exact("playbackentityid", tutorialid).search();

		Collection<String> sectionids = allsections.stream().map(s -> s.getId()).toList();

		Collection<Data> allcomponentwithquestions = getMediaArchive().query("componentcontent").exact("componenttype", "mcq").orgroup("componentsectionid", sectionids).search();

		Collection<String> questionids = allcomponentwithquestions.stream().map(a -> a.get("questionid")).distinct().toList();
		Collection<Data> allquestions = getMediaArchive().query("entityquestion").ids(questionids).search();
		Collection<Data> allanswers = getMediaArchive().query("tutoranswer").orgroup("entityquestion", questionids).exact("user", userid).exact("channel", channelid).search();

		Collection<Map<String, Object>> combined = allquestions.stream().map(q -> {
			Map<String, Object> map = new HashMap<>();
			map.put("question", q);
			Data answer = allanswers.stream().filter(a -> a.get("entityquestion").equals(q.getId())).findFirst().orElse(null);
			map.put("answer", answer);
			return map;
		}).toList();

		Map<String, Double> cognitivelevelpoints = getCognitiveLevelPoints();
		Map<String, Double> answerconfidencebonus = getAnswerConfidenceBonus();

		double confidentbonus = answerconfidencebonus.getOrDefault("confident", 0.0);

		double beginnerpoints_pq = cognitivelevelpoints.getOrDefault("beginner", 0.0);
		beginnerpoints_pq += beginnerpoints_pq * (confidentbonus / 100.0);

		double competentpoints_pq = cognitivelevelpoints.getOrDefault("competent", 0.0);
		competentpoints_pq += competentpoints_pq * (confidentbonus / 100.0);

		double expertpoints_pq = cognitivelevelpoints.getOrDefault("expert", 0.0);
		expertpoints_pq += expertpoints_pq * (confidentbonus / 100.0);

		double outof_batch = Math.max(Math.min(allquestions.size(), 10), allanswers.size() + 1);

		double outof_beginnerpoints = beginnerpoints_pq * outof_batch;
		double outof_competentpoints = competentpoints_pq * outof_batch;
		double outof_expertpoints = expertpoints_pq * outof_batch;

		if (outof_beginnerpoints == 0.0 || outof_competentpoints == 0.0 || outof_expertpoints == 0.0)
		{
			throw new IllegalStateException("Out of points for one or more cognitive levels is zero. This should not happen.");
		}

		double total_beginnerpoints = 0.0;
		double total_competentpoints = 0.0;
		double total_expertpoints = 0.0;

		for (Map<String, Object> map : combined)
		{
			Data question = (Data) map.get("question");
			MultiValued answer = (MultiValued) map.get("answer");

			if (answer != null)
			{
				Double pointsearned = answer.getDouble("pointsearned");
				if (pointsearned == null)
				{
					pointsearned = 0.0;
				}
				Double bonusearned = answer.getDouble("bonusearned");
				if (bonusearned == null)
				{
					bonusearned = 0.0;
				}
				double totalpoints = pointsearned + bonusearned;

				String cognitivelevel = question.get("mcqcognitivelevel");
				if ("beginner".equals(cognitivelevel))
				{
					total_beginnerpoints += totalpoints;
				}
				else if ("competent".equals(cognitivelevel))
				{
					total_competentpoints += totalpoints;
				}
				else if ("expert".equals(cognitivelevel))
				{
					total_expertpoints += totalpoints;
				}
			}
		}

		double average_beginnerpoints = total_beginnerpoints / outof_beginnerpoints;
		double average_competentpoints = total_competentpoints / outof_competentpoints;
		double average_expertpoints = total_expertpoints / outof_expertpoints;

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

		messageContext.setMessageAgentContext("messageid", tutorialid + "_progressupdate");
		messageContext.setMessageAgentContext("messagetype", "progressupdate");
		messageContext.setMessageAgentContext("tutorialid", tutorialid);
		messageContext.setMessageAgentContext("beginnerprogress", String.format("%.4f", average_beginnerpoints));
		messageContext.setMessageAgentContext("competentprogress", String.format("%.4f", average_competentpoints));
		messageContext.setMessageAgentContext("expertprogress", String.format("%.4f", average_expertpoints));

		agentmessage.setValue("message", "Progress updated for tutorial " + tutorialid);

		AgentEnabled skillEnabled = messageContext.getCurrentAgentEnable();
		messageContext.fireStatusComplete(skillEnabled);

	}
}
