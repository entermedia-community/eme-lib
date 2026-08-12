package org.entermediadb.ai.skills;

import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.TutorMessageContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.MultiValued;

public class AdaptiveTutorialEndSkill extends AdaptiveTutorialBaseSkill
{
	@Override
	public void process(AgentContext inAgentContext)
	{
		TutorMessageContext tutorMessageContext = (TutorMessageContext) inAgentContext;

		String tutorialid = (String) tutorMessageContext.getContextValue("tutorialid");
		MultiValued tutorial = (MultiValued) getMediaArchive().query("entitytutorial").exact("id", tutorialid).searchOne();

		tutorMessageContext.putContextValue("tutorial", tutorial);

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender"); // Should stay
		// search_start
		LlmResponse response = llmconnection.renderLocalAction(inAgentContext, "chat_tutor_end");
		// response.setNextSkillEnabled("auto_detect_conversation");
		tutorMessageContext.setLastResponse(response);
		tutorMessageContext.log("sent" + response.getMessagePlain());
		// }
		// super.process(messageContext);

		AgentEnabled skillEnabled = tutorMessageContext.getCurrentAgentEnable();
		tutorMessageContext.fireStatusComplete(skillEnabled);
	}
}
