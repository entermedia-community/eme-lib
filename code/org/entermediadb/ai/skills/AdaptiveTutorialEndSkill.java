package org.entermediadb.ai.skills;

import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.MultiValued;

public class AdaptiveTutorialEndSkill extends AdaptiveTutorialBaseSkill
{
	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;

		messageContext.putContextValue("skiploader", Boolean.TRUE);

		String tutorialid = (String) messageContext.getContextValue("tutorialid");
		MultiValued tutorial = (MultiValued) getMediaArchive().query("entitytutorial").exact("id", tutorialid).searchOne();

		messageContext.putContextValue("tutorial", tutorial);

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender"); // Should stay
		// search_start
		LlmResponse response = llmconnection.renderLocalAction(inAgentContext, "chat_tutor_end");
		// response.setNextSkillEnabled("auto_detect_conversation");
		messageContext.setLastResponse(response);
		messageContext.log("sent" + response.getMessagePlain());
		// }
		// super.process(messageContext);

		AgentEnabled skillEnabled = messageContext.getCurrentAgentEnable();
		messageContext.fireStatusComplete(skillEnabled);
	}
}
