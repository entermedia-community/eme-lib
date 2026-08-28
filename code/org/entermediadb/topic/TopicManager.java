package org.entermediadb.topic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;

public class TopicManager extends BaseMediaModule
{

	public void getUserTopics(WebPageRequest inReq)
	{
		MediaArchive mediaArchive = getMediaArchive(inReq);

		Collection<MultiValued> topics = mediaArchive.query("entitytopic").all().search();

		Collection<Map<String, Object>> data = new ArrayList<>();

		int absoluteTotal = 0;
		int absoluteCompleted = 0;

		for (MultiValued topic : topics)
		{
			Map<String, Object> topicmap = new HashMap<>();

			topicmap.put("topic", topic);

			Collection<MultiValued> tutorials = mediaArchive.query("entitytutorial").exact("entitytopic", topic.getId()).search();
			if (tutorials == null)
			{
				tutorials = new ArrayList<>();
			}

			topicmap.put("tutorials", tutorials.size());

			double beginnerProgress = 0;
			double competentProgress = 0;
			double expertProgress = 0;

			int totalSections = 0;
			int completedSections = 0;

			for (MultiValued tutorial : tutorials)
			{

				MultiValued progress = (MultiValued) mediaArchive.query("tutorialprogress").exact("entitytutorial", tutorial.getId()).exact("user", inReq.getUser().getId()).searchOne();

				if (progress == null)
				{
					continue;
				}

				Double bp = progress.getDouble("beginnerprogress");
				if (bp != null)
				{
					beginnerProgress += bp;
				}
				Double cp = progress.getDouble("competentprogress");
				if (cp != null)
				{
					competentProgress += cp;
				}
				Double ep = progress.getDouble("expertprogress");
				if (ep != null)
				{
					expertProgress += ep;
				}

				Collection<MultiValued> sections = mediaArchive.query("componentsection").exact("playbackentitymoduleid", "entitytutorial").exact("playbackentityid", tutorial.getId()).search();
				totalSections += sections.size();

				Collection<MultiValued> completed = mediaArchive.query("tutorcompletedsection").exact("entitytutorial", tutorial.getId()).exact("user", inReq.getUser().getId()).search();
				completedSections += completed.size();
			}

			Map<String, Double> progressMap = new HashMap<>();
			if (tutorials.size() == 0)
			{
				progressMap.put("beginnerprogress", 0.0);
				progressMap.put("competentprogress", 0.0);
				progressMap.put("expertprogress", 0.0);
			}
			else
			{
				progressMap.put("beginnerprogress", beginnerProgress / tutorials.size());
				progressMap.put("competentprogress", competentProgress / tutorials.size());
				progressMap.put("expertprogress", expertProgress / tutorials.size());
			}

			topicmap.put("progress", progressMap);

			topicmap.put("totalsections", totalSections);
			topicmap.put("completedsections", completedSections);
			data.add(topicmap);

			absoluteTotal += totalSections;
			absoluteCompleted += completedSections;
		}
		inReq.putPageValue("data", data);
		inReq.putPageValue("absolutetotal", absoluteTotal);
		inReq.putPageValue("absolutecompleted", absoluteCompleted);
	}

	public void getTopicTutorials(WebPageRequest inReq)
	{
		String topicid = inReq.getRequestParameter("entitytopic");
		MediaArchive mediaArchive = getMediaArchive(inReq);

		Collection<MultiValued> alltutorials = mediaArchive.query("entitytutorial").exact("entitytopic", topicid).search();

		Collection<Map<String, Object>> data = new ArrayList<>();

		for (MultiValued tutorial : alltutorials)
		{
			Map<String, Object> tutorialmap = new HashMap<>();
			tutorialmap.put("tutorial", tutorial);
			MultiValued progress = (MultiValued) mediaArchive.query("tutorialprogress").exact("entitytutorial", tutorial.getId()).exact("user", inReq.getUser().getId()).searchOne();
			Map<String, Double> progressMap = createProgressMap(progress);
			tutorialmap.put("progress", progressMap);

			Collection<MultiValued> sections = mediaArchive.query("componentsection").exact("playbackentitymoduleid", "entitytutorial").exact("playbackentityid", tutorial.getId()).search();
			int totalSections = sections.size();

			Collection<MultiValued> completed = mediaArchive.query("tutorcompletedsection").exact("entitytutorial", tutorial.getId()).exact("user", inReq.getUser().getId()).search();
			int completedSections = completed.size();

			tutorialmap.put("totalsections", totalSections);
			tutorialmap.put("completedsections", completedSections);
			data.add(tutorialmap);
		}
		inReq.putPageValue("data", data);
	}

	public void loadTutorial(WebPageRequest inReq)
	{
		String tutorialid = inReq.getRequestParameter("entitytutorial");
		MediaArchive mediaArchive = getMediaArchive(inReq);

		MultiValued tutorial = (MultiValued) mediaArchive.query("entitytutorial").exact("id", tutorialid).searchOne();
		inReq.putPageValue("tutorial", tutorial);

		MultiValued progress = (MultiValued) mediaArchive.query("tutorialprogress").exact("entitytutorial", tutorial.getId()).exact("user", inReq.getUser().getId()).searchOne();
		Map<String, Double> progressMap = createProgressMap(progress);
		inReq.putPageValue("progress", progressMap);

		Collection<MultiValued> sections =
			mediaArchive.query("componentsection").exact("playbackentitymoduleid", "entitytutorial").exact("playbackentityid", tutorial.getId()).sort("ordering").search();

		// Collection<Map> sectiondata = new ArrayList<>();

		// for (MultiValued section : sections)
		// {
		// Map sectionmap = new HashMap<>();

		// sectionmap.put("section", section);

		// Collection<MultiValued> componentcontents =
		// mediaArchive.query("componentcontent").exact("componentsectionid",
		// section.getId()).sort("ordering").search();

		// sectionmap.put("componentcontents", componentcontents);

		// sectiondata.add(sectionmap);
		// }

		inReq.putPageValue("sections", sections);
	}

	private Map<String, Double> createProgressMap(MultiValued progress)
	{
		Map<String, Double> progressMap = new HashMap<>();
		if (progress == null)
		{
			progressMap.put("beginnerprogress", 0.0);
			progressMap.put("competentprogress", 0.0);
			progressMap.put("expertprogress", 0.0);
			return progressMap;
		}
		Double bp = progress.getDouble("beginnerprogress");
		if (bp == null)
		{
			bp = 0.0;
		}
		Double cp = progress.getDouble("competentprogress");
		if (cp == null)
		{
			cp = 0.0;
		}
		Double ep = progress.getDouble("expertprogress");
		if (ep == null)
		{
			ep = 0.0;
		}
		progressMap.put("beginnerprogress", bp);
		progressMap.put("competentprogress", cp);
		progressMap.put("expertprogress", ep);
		return progressMap;
	}

}
