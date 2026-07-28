package org.entermediadb.ai.skills;

import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.TutorMessageContext;
import org.entermediadb.ai.automation.RunningScenario;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.MultiValued;

public class AdaptiveTutorialWelcomeSkill extends AdaptiveTutorialBaseSkill
{
	@Override
	public void process(AgentContext inAgentContext)
	{
		TutorMessageContext messageContext = (TutorMessageContext) inAgentContext;

		messageContext.putContextValue("skiploader", Boolean.TRUE);

		String tutorialid = (String) messageContext.getContextValue("tutorialid");
		messageContext.setTutorialId(tutorialid);

		MultiValued tutorial = (MultiValued) getMediaArchive().query("entitytutorial").exact("id", tutorialid).searchOne();

		messageContext.putContextValue("tutorial", tutorial);

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender"); // Should stay
		// search_start
		LlmResponse response = llmconnection.renderLocalAction(inAgentContext, "chat_tutor_welcome");
		// response.setNextSkillEnabled("auto_detect_conversation");
		messageContext.setLastResponse(response);
		messageContext.log("sent" + response.getMessagePlain());
		// }
		// super.process(messageContext);

		messageContext.setLastSectionId(null);
		messageContext.setLastComponentId(null);

		AgentEnabled skillEnabled = messageContext.getCurrentAgentEnable();
		messageContext.fireStatusComplete(skillEnabled);

		RunningScenario scenario = messageContext.getCurrentScenario();

		AgentEnabled nextAgentEnabled = scenario.findEnabled("chat_tutor_continue");
		AgentContext nextContext = scenario.createAgentContext(messageContext, nextAgentEnabled);
		scenario.runProcess(nextAgentEnabled, nextContext, true);
	}

}
