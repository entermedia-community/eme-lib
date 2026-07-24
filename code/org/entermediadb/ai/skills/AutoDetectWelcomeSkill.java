package org.entermediadb.ai.skills;

import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.MultiValued;

public class AutoDetectWelcomeSkill extends BaseSkill
{

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;

		MultiValued agentmessage = messageContext.getAgentMessage();
		if (messageContext.getContextValue("sentwelcome") == null)
		{
			messageContext.putContextValue("sentwelcome", true);
			agentmessage.setValue("chatmessagestatus", "completed");

			LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
			LlmResponse response = llmconnection.renderLocalAction(inAgentContext, "chat_detection_welcome");

			messageContext.setLastResponse(response);
			messageContext.log("sent" + response.getMessagePlain());
		}
		// super.process(messageContext);

		AgentEnabled skillEnabled = messageContext.getCurrentAgentEnable();
		messageContext.fireStatusComplete(skillEnabled);
	}

}
