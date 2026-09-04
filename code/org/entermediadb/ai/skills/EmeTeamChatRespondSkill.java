package org.entermediadb.ai.skills;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.util.ArrayList;
import java.util.Collection;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.automation.RunningScenario;
import org.entermediadb.ai.llm.AutomationStep;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;

public class EmeTeamChatRespondSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(EmeTeamChatRespondSkill.class);

	@Override
	public void startupScenario(AgentContext inContext)
	{
		// super.startupScenario(inContext);
		// dont send hi
	}

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;
		MultiValued agentmessage = messageContext.getAgentMessage();

		MultiValued usermessage = (MultiValued) getMediaArchive().getCachedData("chatterbox", agentmessage.get("replytoid"));

		String query = usermessage.get("message");

		// reset messagereload
		inAgentContext.putContextValue("messagereload", false);

		//TODO Fix entityid for collections?
		String entityid = inAgentContext.get("entityid");

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("thinking");
		LlmResponse response = llmconnection.callToolsFunction(inAgentContext, "emeteamchat_detect");

		log.info(response.getRawResponse());

		messageContext.setLastResponse(response);

		String selectedtool = response.getRunSkillEnabled();
		if (selectedtool == null)
		{
			log.error("No tool selected for query: " + query);
			return;
		}
		String scenario = null;
		if (!selectedtool.contains("."))
		{
			log.error("Selected tool needs the format: scenario.skillenabled. Selected tool:" + selectedtool);
			return;
		}
		scenario = selectedtool.split("\\.")[0];
		String skillenableid = selectedtool.split("\\.")[1];

		log.info("Selected tool: " + selectedtool + " for scenario: " + scenario + " and skill: " + skillenableid);

		JSONObject functionArgs = response.getFunctionArguments();
		inAgentContext.addContext("messagestructured", response.getMessageStructured());
		inAgentContext.addContext("userquery", query);
		inAgentContext.addContext("arguments", functionArgs);

		//we are on a task, or answering questions or another sceneration. 
		if (scenario != null)
		{
			RunningScenario running = (RunningScenario) getMediaArchive().getBean("runningscenario", false);
			running.setId(scenario);

			AutomationStep skillEnabled = running.findEnabled(skillenableid);
			if (skillEnabled == null)
			{
				log.error("No skill enabled found for id: " + skillenableid);
				return;
			}
			running.runProcess(skillEnabled, inAgentContext);
		}
		else
		{
			log.error("Probem");
		}

	}

}
