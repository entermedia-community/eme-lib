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
import org.json.simple.JSONArray;
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
		// TODO Auto-generated method stub
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;
		MultiValued agentmessage = messageContext.getAgentMessage();
		// MultiValued currentfunction = messageContext.getCurrentFunction();

		MultiValued usermessage = (MultiValued) getMediaArchive().getCachedData("chatterbox", agentmessage.get("replytoid"));
		String query = usermessage.get("message");

		//String agentFn = inAgentContext.getCurrentAgentEnable().getAutomationEnabledData().getId();

		inAgentContext.put("userquery", query);

		//Collection<Data> toplevelfunctions = getMediaArchive().query("aifunction").exact("toplevel", true).search();
		//inAgentContext.put("toplevelfunctions", toplevelfunctions);

		Collection<Data> channelchathistory = (Collection<Data> ) inAgentContext.getContextValue("channelchathistory");

		if (channelchathistory == null || channelchathistory.isEmpty())
		{
			//Firstime message
			LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
			LlmResponse response = llmconnection.renderLocalAction(inAgentContext, "emechat_respond_welcome");
			response.setNextSkillEnabled("emechat_responder_welcome");
			messageContext.setLastResponse(response);
			AgentEnabled skillEnabled = messageContext.getCurrentAgentEnable();
			messageContext.fireStatusComplete(skillEnabled);
			return;
		}

		String entityid = inAgentContext.get("entityid");
		
		Data entity = getMediaArchive().getCachedData("collectiveproject", entityid);
		String collectionid = entity.get("parentcollectionid");

		Collection<MultiValued> teamUsers = getMediaArchive().query("librarycollectionusers").exact("collectionid", collectionid).exact("ontheteam", "true").exists("teamroles").search();
		Collection<Data> roles = new ArrayList<Data>();
		for (MultiValued memeber : teamUsers)
		{
			Collection<String> roleids = memeber.getValues("teamroles");
			for (String roleid : roleids)
			{
				Data role = getMediaArchive().getCachedData("collectiverole", roleid);
				roles.add(role);
			}
		}
		messageContext.putContextValue("roles", roles);

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("thinking");
		LlmResponse response = llmconnection.callToolsFunction(inAgentContext, "emechat_detect");

		log.info(response.getRawResponse());

		messageContext.setLastResponse(response);

		String selectedtool = response.getRunSkillEnabled();
		if (selectedtool == null)
		{
			log.error("No tool selected for query: " + query);
			return;
		}
		String scheario = selectedtool.split("\\.")[0];
		String skillenableid = selectedtool.split("\\.")[1];

		JSONObject functionArgs = response.getFunctionArguments();
		inAgentContext.addContext("messagestructured", response.getMessageStructured());
		inAgentContext.addContext("userquery", query);
		inAgentContext.addContext("arguments", functionArgs);

		
		if (scheario != null)
		{

			RunningScenario running = (RunningScenario) getMediaArchive().getBean("runningscenario", false);;
			running.setId(scheario);
			
			AgentEnabled skillEnabled = running.findEnabled(skillenableid);
			AgentContext childContext = running.createAgentContext(inAgentContext, skillEnabled);
			childContext.setCurrentScenario(running);

			childContext.putContextValue("cancelstartup" + skillenableid, true);
			childContext.putContextValue("cancelemptyresponse", true);
			
			childContext.setValue("entityid", collectionid);
			childContext.setValue("entitymoduleid", "librarycollection");

			//Get the docids for the collection and set it in the context
			Collection<String> docids = getAssistantManager().findDocIdsForEntity("librarycollection", collectionid);
			childContext.putContextValue("docids", docids);

			String responserole = (String) response.getMessageStructured().get("role");
			if (responserole == null || responserole.isEmpty())
			{
				log.error("No role selected for query: " + query);
				return;
			}
			
			
			String roleid = null;
			for (Data role : roles)
			{
				if (role.getName().equals(responserole))
				{
					roleid = role.getId();
					break;
				}
			}
			String emeprofileid = null;
			String useralias = null;
			for (MultiValued memeber : teamUsers)
			{
				if (memeber.containsValue("teamroles", roleid))
				{
					String userid = memeber.get("followeruser");
					Data emeprofile = getMediaArchive().query("emeprofile").exact("owner", memeber.get("followeruser")).searchOne();
					if (emeprofile != null)
					{
						emeprofileid = emeprofile.getId();
						useralias = userid;
					}
					break;
				}
			}

			if (emeprofileid != null)
			{
				Collection<String> profiledocids = getAssistantManager().findDocIdsForEntity("emeprofile", emeprofileid);
				docids.addAll(profiledocids);
			}
			agentmessage.setValue("useralias", useralias);

			running.runProcess(skillEnabled, childContext);
		}


	}

}
