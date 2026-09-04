package org.entermediadb.ai.skills;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.TutorMessageContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.asset.Asset;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;

public class AdaptiveTutorialContinueSkill extends AdaptiveTutorialBaseSkill
{
	@Override
	public void process(AgentContext inAgentContext)
	{
		TutorMessageContext tutorMessageContext = (TutorMessageContext) inAgentContext;

		String tutorialid = (String) tutorMessageContext.getContextValue("tutorialid");
		String sectionid = (String) tutorMessageContext.getContextValue("sectionid");
		String componentid = (String) tutorMessageContext.getContextValue("componentid");

		Object _isDailyChallenge = tutorMessageContext.getContextValue("isdailychallenge");
		Boolean isDailyChallenge = null;
		if (_isDailyChallenge instanceof String)
		{
			isDailyChallenge = Boolean.parseBoolean((String) _isDailyChallenge);
		}
		else
		{
			isDailyChallenge = (Boolean) _isDailyChallenge;
		}
		if (isDailyChallenge == null)
		{
			isDailyChallenge = false;
		}

		Collection<String> questionIds = Collections.emptyList();
		if (sectionid != null)
		{
			Collection<Data> answeredQuestions = getMediaArchive().query("tutoranswer").exact("user", tutorMessageContext.getUserProfile().getUser().getId()).search();
			questionIds = answeredQuestions.stream().map(a -> a.get("entityquestion")).toList();
		}

		int questionspersection = 1;
		String qpsStr = getMediaArchive().getCatalogSettingValue("questions-per-section");
		if (qpsStr != null && !qpsStr.trim().isEmpty())
		{
			try
			{
				questionspersection = Integer.parseInt(qpsStr.trim());
			}
			catch (Exception e)
			{
				questionspersection = 1;
			}
		}

		while (true)
		{

			Map<String, Data> next = getNextSectionAndComponent(tutorialid, sectionid, componentid, questionIds, questionspersection, isDailyChallenge);
			if (next == null)
			{
				endTutorial(tutorMessageContext);
				return;
			}

			Data topsection = next.get("section");
			Data topcomponent = next.get("component");

			if (topsection == null || topcomponent == null)
			{
				endTutorial(tutorMessageContext);
				return;
			}

			if (componentid != null && componentid.equals(topcomponent.getId()))
			{
				throw new IllegalStateException("Next component is the same as the current component. This should not happen.");
			}

			tutorMessageContext.putContextValue("sectionid", topsection.getId());
			tutorMessageContext.putContextValue("componentid", topcomponent.getId());

			tutorMessageContext.putContextValue("componentcontent", topcomponent.get("content"));
			tutorMessageContext.putContextValue("componenttype", topcomponent.get("componenttype"));
			tutorMessageContext.putContextValue("contentrole", topcomponent.get("contentrole"));

			if ("mcq".equals(topcomponent.get("componenttype")))
			{
				if (topcomponent.get("questionid") != null)
				{
					Data question = getMediaArchive().getData("entityquestion", topcomponent.get("questionid"));
					JSONObject questionjson = new JSONObject();
					questionjson.put("id", question.getId());
					questionjson.put("question", question.get("question"));

					JSONObject options = new JSONObject();
					options.put("option_a", question.get("option_a"));
					options.put("option_b", question.get("option_b"));
					options.put("option_c", question.get("option_c"));
					options.put("option_d", question.get("option_d"));
					options.put("option_e", question.get("option_e"));
					options.put("option_f", question.get("option_f"));
					questionjson.put("options", options);

					questionjson.put("correctoption", question.get("correctoption"));
					questionjson.put("rationale", question.get("rationale"));
					questionjson.put("mcqcognitivelevel", question.get("mcqcognitivelevel"));
					tutorMessageContext.putContextValue("question", questionjson);
				}
				tutorMessageContext.putContextValue("messagerendertype", "question");
				tutorMessageContext.putContextValue("interactive", "yes");
			}
			else if ("asset".equals(topcomponent.get("componenttype")))
			{
				if (topcomponent.get("assetid") != null)
				{
					Asset asset = getMediaArchive().getAsset(topcomponent.get("assetid"));
					if (asset == null)
					{
						// set some fallback no-img thumbnail
					}
					else
					{
						JSONObject assetMap = new JSONObject();
						assetMap.put("id", asset.getId());
						String siteroot = (String) tutorMessageContext.getContextValue("siteroot");
						if (siteroot == null)
						{
							siteroot = "";
						}

						String mediatype = getMediaArchive().getMediaRenderType(asset);
						assetMap.put("mediatype", mediatype);
						if ("video".equals(mediatype))
						{
							String source = getMediaArchive().asLinkToGenerated(asset, "video.m3u8");
							String sourceMp4 = getMediaArchive().asLinkToGenerated(asset, "video.mp4");
							Collection<String> sources = asset.getValues("hlsstreams");
							String url = null;
							if (sources != null)
							{
								String maxSource = sources.stream().max((s1, s2) -> {
									int size1 = Integer.parseInt(s1);
									int size2 = Integer.parseInt(s2);
									return Integer.compare(size1, size2);
								}).orElse(null);
								if (maxSource != null)
									url = source + "/" + maxSource + "/video.m3u8";
							}
							if (url == null)
							{
								url = sourceMp4;
							}
							assetMap.put("url", siteroot + url);
							String assetthumbnail = siteroot + getMediaArchive().asLinkToPreview(asset, "image1900x1080");
							assetMap.put("thumbnail", assetthumbnail);
						}
						else
						{
							String assetthumbnail = siteroot + getMediaArchive().asLinkToPreview(asset, "image200x200");
							assetMap.put("thumbnail", assetthumbnail);

							String asseturl = siteroot + getMediaArchive().asLinkToPreview(asset, "image3000x3000");
							assetMap.put("url", asseturl);
						}
						tutorMessageContext.putContextValue("asset", assetMap);
					}
				}
				tutorMessageContext.putContextValue("messagerendertype", "asset");
			}
			else
			{
				tutorMessageContext.putContextValue("messagerendertype", "text");
			}

			AgentEnabled skillEnabled = tutorMessageContext.getCurrentAgentEnable();
			tutorMessageContext.fireStatusComplete(skillEnabled);

			if (shouldPause(topcomponent))
			{
				tutorMessageContext.setWaitTime(null);
				return;
			}

			sectionid = topsection.getId();
			componentid = topcomponent.getId();

			Map<String, Data> hasNext = getNextSectionAndComponent(tutorialid, sectionid, componentid, questionIds, questionspersection, isDailyChallenge);
			if (hasNext == null)
			{
				endTutorial(tutorMessageContext);
				return;
			}

			MultiValued newMessage = (MultiValued) getMediaArchive().getSearcher("chatterbox").createNewData();
			newMessage.setJSONValue("agentcontextvalues", tutorMessageContext.toJSON());
			newMessage.setValue("date", new Date());
			newMessage.setValue("channel", tutorMessageContext.getChannel().getId());
			newMessage.setValue("user", "agent");

			tutorMessageContext.setAgentMessage(newMessage);
			tutorMessageContext.putContextValue("sectionid", sectionid);
			tutorMessageContext.putContextValue("componentid", componentid);
			tutorMessageContext.putContextValue("tutorialid", tutorialid);
		}
	}

	public Map<String, Data> getNextSectionAndComponent(String tutorialid, String sectionid, String componentid, Collection<String> questionIds, int questionspersection, boolean isDailyChallenge)
	{
		Data currentsection = null;
		if (sectionid != null)
		{
			currentsection = getMediaArchive().getData("componentsection", sectionid);
		}
		else
		{
			currentsection = getMediaArchive().query("componentsection").exact("playbackentitymoduleid", "entitytutorial").exact("playbackentityid", tutorialid).sort("ordering").searchOne();
		}

		if (currentsection == null)
		{
			return null;
		}

		MultiValued currentSectionMv = (MultiValued) currentsection;
		int currentSectionOrdering = currentSectionMv.getInt("ordering");

		Data nextSection = null;
		if (!isDailyChallenge)
		{
			nextSection = getMediaArchive().query("componentsection")
				.exact("playbackentitymoduleid", "entitytutorial")
				.exact("playbackentityid", tutorialid)
				.moreThan("ordering", currentSectionOrdering)
				.sort("ordering")
				.searchOne();
		}

		if (nextSection != null)
		{
			Collection<Data> currentSecComponents = getMediaArchive().query("componentcontent").exact("componentsectionid", currentsection.getId()).sort("ordering").search();
			Collection<String> currentSecQuestionIds = currentSecComponents.stream().filter(data -> data.get("questionid") != null).map(data -> data.get("questionid")).collect(Collectors.toList());
			Collection<Data> answers = getMediaArchive().query("tutoranswer").orgroup("entityquestion", currentSecQuestionIds).search();

			if (answers.size() >= questionspersection)
			{
				return getNextSectionAndComponent(tutorialid, nextSection.getId(), null, questionIds, questionspersection, isDailyChallenge);
			}
		}

		Data nextcomponent = null;
		if (componentid != null)
		{
			MultiValued currentcomponent = (MultiValued) getMediaArchive().getData("componentcontent", componentid);
			if (currentcomponent != null)
			{
				int currentOrdering = currentcomponent.getInt("ordering");
				nextcomponent = getMediaArchive().query("componentcontent")
					.exact("componentsectionid", currentsection.getId())
					.notgroup("questionid", questionIds)
					.moreThan("ordering", currentOrdering)
					.sort("ordering")
					.searchOne();
			}
		}
		else
		{
			nextcomponent = getMediaArchive().query("componentcontent").exact("componentsectionid", currentsection.getId()).notgroup("questionid", questionIds).sort("ordering").searchOne();
		}

		if (nextcomponent == null)
		{
			if (nextSection == null)
			{
				return null;
			}
			else
			{
				return getNextSectionAndComponent(tutorialid, nextSection.getId(), null, questionIds, questionspersection, isDailyChallenge);
			}
		}

		Map<String, Data> result = new HashMap<String, Data>();
		result.put("section", currentsection);
		result.put("component", nextcomponent);
		return result;

	}

	Boolean shouldPause(Data component)
	{
		if ("exercise".equals(component.get("contentrole")))
		{
			return true;
		}
		if ("asset".equals(component.get("componenttype")) || "mcq".equals(component.get("componenttype")))
		{
			return true;
		}
		return false;
	}

}
