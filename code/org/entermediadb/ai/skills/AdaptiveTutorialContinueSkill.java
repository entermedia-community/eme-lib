package org.entermediadb.ai.skills;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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
		TutorMessageContext messageContext = (TutorMessageContext) inAgentContext;

		String tutorialid = messageContext.getTutorialId();
		String sectionid = (String) messageContext.getMessageAgentContext("sectionid");
		String componentid = (String) messageContext.getMessageAgentContext("componentid");

		Map<String, Data> next = getNextSectionAndComponent(tutorialid, sectionid, componentid);
		if (next == null)
		{
			endTutorial(messageContext);
			return;
		}

		Data topsection = next.get("section");
		Data topcomponent = next.get("component");

		if (topsection == null || topcomponent == null)
		{
			endTutorial(messageContext);
			return;
		}

		if (componentid != null && componentid.equals(topcomponent.getId()))
		{
			throw new IllegalStateException("Next component is the same as the current component. This should not happen.");
		}

		messageContext.setMessageAgentContext("sectionid", topsection.getId());
		messageContext.setMessageAgentContext("componentid", topcomponent.getId());

		messageContext.setMessageAgentContext("componentcontent", topcomponent.get("content"));
		messageContext.setMessageAgentContext("componenttype", topcomponent.get("componenttype"));
		messageContext.setMessageAgentContext("contentrole", topcomponent.get("contentrole"));

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

				// questionjson.put("correctoption", question.get("correctoption"));
				// questionjson.put("rationale", question.get("rationale"));
				questionjson.put("mcqcognitivelevel", question.get("mcqcognitivelevel"));
				messageContext.setMessageAgentContext("question", questionjson);
			}
			messageContext.setMessageAgentContext("messagetype", "question");
			messageContext.setMessageAgentContext("interactive", "yes");
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
					String siteroot = (String) messageContext.getContextValue("siteroot");

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
					messageContext.setMessageAgentContext("asset", assetMap);
				}
			}
			messageContext.setMessageAgentContext("messagetype", "asset");
		}
		else
		{
			messageContext.setMessageAgentContext("messagetype", "text");
		}

		AgentEnabled skillEnabled = messageContext.getCurrentAgentEnable();
		messageContext.fireStatusComplete(skillEnabled);
	}

	public Map<String, Data> getNextSectionAndComponent(String tutorialid, String sectionid, String componentid)
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

		Data nextSection = getMediaArchive().query("componentsection")
			.exact("playbackentitymoduleid", "entitytutorial")
			.exact("playbackentityid", tutorialid)
			.moreThan("ordering", currentSectionOrdering)
			.sort("ordering")
			.searchOne();

		Data nextcomponent = null;
		if (componentid != null)
		{
			MultiValued currentcomponent = (MultiValued) getMediaArchive().getData("componentcontent", componentid);
			if (currentcomponent != null)
			{
				int currentOrdering = currentcomponent.getInt("ordering");
				nextcomponent = getMediaArchive().query("componentcontent").exact("componentsectionid", currentsection.getId()).moreThan("ordering", currentOrdering).sort("ordering").searchOne();
			}
		}
		else
		{
			nextcomponent = getMediaArchive().query("componentcontent").exact("componentsectionid", currentsection.getId()).sort("ordering").searchOne();
		}

		if (nextcomponent == null)
		{
			if (nextSection == null)
			{
				return null;
			}
			else
			{
				return getNextSectionAndComponent(tutorialid, nextSection.getId(), null);
			}
		}

		Map<String, Data> result = new HashMap<String, Data>();
		result.put("section", currentsection);
		result.put("component", nextcomponent);
		return result;
	}

}
