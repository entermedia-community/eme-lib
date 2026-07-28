package org.entermediadb.ai.skills;

import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.creator.AiSmartCreatorSteps;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.json.simple.JSONObject;
import org.openedit.Data;

public class SmartCreatorParseUserPromptSkill extends BaseSkill
{

	@Override
	public void process(AgentContext inContext)
	{
		// ChatMessageContext messageContext = (ChatMessageContext) createAgentContext(inContext,
		// inContext.getCurrentAgentEnable());
		ChatMessageContext messageContext = (ChatMessageContext) inContext;
		parseUserPrompt(messageContext); // Calls smartcreator_createoutline
		super.process(messageContext);
	}

	public LlmResponse parseUserPrompt(ChatMessageContext messageContext)
	{
		Data usermessage = getMediaArchive().getCachedData("chatterbox", messageContext.getAgentMessage().get("replytoid"));

		String prompt = usermessage.get("message");

		LlmResponse response = parseCreationPrompt(messageContext, prompt);
		return response;
	}

	public LlmResponse parseCreationPrompt(AgentContext messageContext, String prompt)
	{
		// instructions come from SmartCreatorMakeSuggestionsSkill.makeSuggestions
		AiSmartCreatorSteps instructions = messageContext.getAiSmartCreatorSteps();
		messageContext.addContext("creationprompt", prompt);
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("thinking");
		LlmResponse res = llmconnection.callStructure(messageContext, "smartcreator_parse");

		JSONObject paragraphs = res.getMessageStructured();
		instructions.loadJsonParts(paragraphs);

		return res;
	}
}
