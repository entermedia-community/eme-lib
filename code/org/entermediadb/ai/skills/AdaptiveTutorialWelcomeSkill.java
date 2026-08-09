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
		TutorMessageContext messageContext = (TutorMessageContext) inAgentContext;

		String channelid = messageContext.getChannel().getId();
		HitTracker messages = getMediaArchive().query("chatterbox").exact("channel", channelid).not("messagetype", "system").search();
		if (messages.size() > 0)
		{
			return;
		}

		String tutorialid = (String) messageContext.getContextValue("tutorialid");

		MultiValued tutorial = (MultiValued) getMediaArchive().query("entitytutorial").exact("id", tutorialid).searchOne();

		messageContext.putContextValue("tutorial", tutorial);

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
		LlmResponse response = llmconnection.renderLocalAction(inAgentContext, "chat_tutor_welcome");
		messageContext.setLastResponse(response);
		messageContext.log("sent" + response.getMessagePlain());

		messageContext.setMessageAgentContext("sectionid", null);
		messageContext.setMessageAgentContext("componentid", null);
		messageContext.setMessageAgentContext("messagetype", "welcome");

		AgentEnabled skillEnabled = messageContext.getCurrentAgentEnable();
		messageContext.fireStatusComplete(skillEnabled);

		// RunningScenario scenario = messageContext.getCurrentScenario();

		// AgentEnabled nextAgentEnabled = scenario.findEnabled("chat_tutor_continue");
		// TutorMessageContext nextContext = (TutorMessageContext)
		// scenario.createAgentContext(messageContext, nextAgentEnabled);
		// nextContext.setWaitTime(200l);
		// scenario.runProcess(nextAgentEnabled, nextContext, true);
	}

}
