package org.entermediadb.ai.skills;

import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.TutorMessageContext;
import org.entermediadb.ai.llm.AutomationStep;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.hittracker.HitTracker;

public class AdaptiveTutorialWelcomeSkill extends AdaptiveTutorialBaseSkill
{
	@Override
	public void process(AgentContext inAgentContext)
	{
		TutorMessageContext tutorMessageContext = (TutorMessageContext) inAgentContext;

		String channelid = tutorMessageContext.getChannel().getId();
		HitTracker messages = getMediaArchive().query("chatterbox").exact("channel", channelid).not("messagetype", "system").search();
		if (messages.size() > 0)
		{
			return;
		}

		String tutorialid = null;

		Boolean isdailychallenge = Boolean.parseBoolean((String) tutorMessageContext.getContextValue("isdailychallenge"));

		if (isdailychallenge == null || !isdailychallenge)
		{
			tutorMessageContext.putContextValue("sectionid", null);
			tutorialid = (String) tutorMessageContext.getContextValue("tutorialid");
		}
		else
		{
			String sectionid = (String) tutorMessageContext.getContextValue("sectionid");
			Data section = getMediaArchive().query("componentsection").exact("id", sectionid).searchOne();
			tutorialid = section.get("playbackentityid");
			inAgentContext.putContextValue("isdailychallenge", true);
		}

		MultiValued tutorial = (MultiValued) getMediaArchive().query("entitytutorial").exact("id", tutorialid).searchOne();

		tutorMessageContext.putContextValue("tutorial", tutorial);

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
		LlmResponse response = llmconnection.renderLocalAction(inAgentContext, "chat_tutor_welcome");
		tutorMessageContext.setLastResponse(response);
		tutorMessageContext.log("sent" + response.getMessagePlain());

		tutorMessageContext.putContextValue("componentid", null);
		tutorMessageContext.putContextValue("messagerendertype", "welcome");

		AutomationStep skillEnabled = tutorMessageContext.getCurrentAgentEnable();
		tutorMessageContext.fireStatusComplete(skillEnabled);
	}

}
