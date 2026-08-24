package org.entermediadb.ai.skills;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.automation.RunningScenario;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;

public class EmeChatDetectSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(EmeChatDetectSkill.class);

	@Override
	public void startupScenario(AgentContext inContext)
	{
		// super.startupScenario(inContext);
		// dont send hi
		log.info("Starting chat detect skill");
	}

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;
		MultiValued agentmessage = messageContext.getAgentMessage();

		MultiValued usermessage = (MultiValued) getMediaArchive().getCachedData("chatterbox", agentmessage.get("replytoid"));
		String query = usermessage.get("message");

		inAgentContext.put("userquery", query);

		Collection<Data> channelchathistory = (Collection<Data>) inAgentContext.getContextValue("channelchathistory");

		String channeltypeskill = "emeteamchat";
		String channeltype = messageContext.getChannel().get("channeltype");
		if ("emechat".equals(channeltype))
		{
			channeltypeskill = "emechat";
			// Get the user EME profile and set it as the alias of the Agent
			Data emeprofile = getAssistantManager().getEmeProfileForUser(usermessage.get("user"));
			if (emeprofile != null)
			{
				String useralias = emeprofile.get("owner");
				agentmessage.setValue("useralias", useralias);
			}
		}

		if (channelchathistory == null || channelchathistory.isEmpty())
		{
			// Firstime message
			LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
			LlmResponse response = llmconnection.renderLocalAction(inAgentContext, channeltypeskill + "_respond_welcome");
			response.setNextSkillEnabled(channeltypeskill + "_responder_respond");
			messageContext.setLastResponse(response);
			AgentEnabled skillEnabled = messageContext.getCurrentAgentEnable();
			messageContext.fireStatusComplete(skillEnabled);
			return;
		}

	}

}
