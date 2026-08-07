package org.entermediadb.ai.skills;

import java.util.HashMap;
import java.util.Map;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.TutorMessageContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.Data;
import org.openedit.MultiValued;

public class AdaptiveTutorialContinueSkill extends AdaptiveTutorialBaseSkill
{
	@Override
	public void process(AgentContext inAgentContext)
	{
		TutorMessageContext messageContext = (TutorMessageContext) inAgentContext;

		String tutorialid = messageContext.getTutorialId();
		String sectionid = (String) messageContext.getLastSectionId();
		String componentid = (String) messageContext.getLastComponentId();

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

		messageContext.setLastSectionId(topsection.getId());
		messageContext.setLastComponentId(topcomponent.getId());

		messageContext.putContextValue("topcomponent", topcomponent);

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
		LlmResponse response = llmconnection.renderLocalAction(messageContext, "chat_tutor_continue");

		messageContext.setLastResponse(response);
		messageContext.log("sent" + response.getMessage());

		if ("mcq".equals(topcomponent.get("componenttype")))
		{
			messageContext.setMessageAgentContext("messagetype", "question");
			messageContext.setMessageAgentContext("interactive", "yes");
		}
		else if ("asset".equals(topcomponent.get("componenttype")))
		{
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
