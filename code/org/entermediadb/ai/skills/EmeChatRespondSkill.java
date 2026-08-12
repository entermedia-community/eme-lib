package org.entermediadb.ai.skills;

import java.util.Collection;

import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.Data;

public class EmeChatRespondSkill extends BaseSkill
{
	@Override
	public void startupScenario(AgentContext inContext)
	{
		// super.startupScenario(inContext);
		// dont send hi
	}

	@Override
	public void process(AgentContext inAgentContext)
	{
		// TODO Auto-generated method stub
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;
		
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
		LlmResponse response = llmconnection.renderLocalAction(inAgentContext, "emechat_respond");
		
		messageContext.setLastResponse(response);

		// Check the messages and respond if needed
		Collection<Data> history = (Collection<Data>) inAgentContext.getContextValue("channelchathistory");

		if (history != null && !history.isEmpty())
		{
			// get last user messages and try and respond to them
			// call a structured prompt to determine what they are needing

			// Once parsed call super.process to continue the scenario

			// Pass in all the users and determin who should respond. If no one should respond then just
			// continue the scenario without responding.

			// Use that tone of voice

			// We may not need to resond at all if the user is just chatting and not asking for anything. We can
			// just continue the scenario without responding.
			super.process(inAgentContext);

		}

	}

}
