package org.entermediadb.ai.skills;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.llm.AutomationStep;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.json.simple.JSONObject;
import org.openedit.MultiValued;

public class ChatMonitorSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(ChatMonitorSkill.class);

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;
		MultiValued agentmessage = messageContext.getAgentMessage();
		// MultiValued currentfunction = messageContext.getCurrentFunction();

		MultiValued usermessage = (MultiValued) getMediaArchive().getCachedData("chatterbox", agentmessage.get("replytoid"));
		String query = usermessage.get("message");

		String agentFn = inAgentContext.getCurrentAgentEnable().getAutomationStepData().getId();

		// Move to its own skill, next step is parse text
		inAgentContext.put("userquery", query);

		// Collection<Data> toplevelfunctions = getMediaArchive().query("aifunction").exact("toplevel",
		// true).search();
		// inAgentContext.put("toplevelfunctions", toplevelfunctions);

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("thinking");

		LlmResponse response = llmconnection.callToolsFunction(inAgentContext, agentFn);

		log.info(response.getRawResponse());

		String skillenableid = response.getRunSkillEnabled();
		JSONObject functionArgs = response.getFunctionArguments();

		inAgentContext.addContext("messagestructured", response.getMessageStructured());
		inAgentContext.addContext("userquery", query);
		inAgentContext.addContext("arguments", functionArgs);

		if (skillenableid.equals("auto_detect_showresponse"))
		{
			llmconnection = getMediaArchive().getLlmConnection("localrender"); // Should stay search_start
			response = llmconnection.renderLocalAction(inAgentContext, "auto_detect_showresponse");
		}
		else
		{
			response.setRunSkillEnabled(skillenableid);
		}
		messageContext.setLastResponse(response);

		AutomationStep skillEnabled = messageContext.getCurrentAgentEnable();
		messageContext.fireStatusComplete(skillEnabled);
	}

}
