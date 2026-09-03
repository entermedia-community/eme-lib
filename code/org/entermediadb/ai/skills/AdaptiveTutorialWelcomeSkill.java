package org.entermediadb.ai.skills;

import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.TutorMessageContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
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

		String tutorialid = (String) tutorMessageContext.getContextValue("tutorialid");

		MultiValued tutorial = (MultiValued) getMediaArchive().query("entitytutorial").exact("id", tutorialid).searchOne();

		tutorMessageContext.putContextValue("tutorial", tutorial);

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
		LlmResponse response = llmconnection.renderLocalAction(inAgentContext, "chat_tutor_welcome");
		tutorMessageContext.setLastResponse(response);
		tutorMessageContext.log("sent" + response.getMessagePlain());

		tutorMessageContext.putContextValue("sectionid", null);
		tutorMessageContext.putContextValue("componentid", null);
		tutorMessageContext.putContextValue("messagerendertype", "welcome");

		AgentEnabled skillEnabled = tutorMessageContext.getCurrentAgentEnable();
		tutorMessageContext.fireStatusComplete(skillEnabled);
	}

}
