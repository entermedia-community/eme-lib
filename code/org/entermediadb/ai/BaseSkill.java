package org.entermediadb.ai;

import java.util.ArrayList;
import java.util.Collection;
import org.entermediadb.ai.assistant.AssistantManager;
import org.entermediadb.ai.llm.AgentEnabled;
import org.openedit.CatalogEnabled;
import org.openedit.Data;

public class BaseSkill extends BaseAiManager implements Skill, CatalogEnabled
{
	public void startupScenario(AgentContext inContext)
	{
		Boolean cancelStarting = (Boolean) inContext.getContextValue("cancelstartup" + inContext.getCurrentAgentEnable().getEnabledId());
		if (cancelStarting != null && cancelStarting.booleanValue())
		{
			return;
		}
		AgentEnabled skillEnabled = inContext.getCurrentAgentEnable();
		inContext.fireStatusStarting(skillEnabled);

	}

	public void endScenario(AgentContext inContext)
	{
		// Dont run the end event if the process was skipped beause it ends when the next starts
	}

	/**
	 * This is the main process method that will be called by the agent to keep processing the children.
	 */
	@Override
	public void process(AgentContext inContext)
	{
		AgentEnabled skillEnabled = inContext.getCurrentAgentEnable();
		inContext.fireStatusComplete(skillEnabled);

		Collection<AgentEnabled> children = inContext.getCurrentAgentEnable().getChildren();

		for (AgentEnabled agentEnabled : children)
		{
			AgentContext childContext = inContext.getCurrentScenario().createAgentContext(inContext, agentEnabled);

			// agentEnabled.getAgent().processstart(childContext);
			inContext.getCurrentScenario().runProcess(agentEnabled, childContext);
			// agentEnabled.getAgent().processend(childContext);
		}
	}

	public AssistantManager getAssistantManager()
	{
		AssistantManager assistantManager = (AssistantManager) getMediaArchive().getBean("assistantManager");
		return assistantManager;
	}

	protected Collection<String> loadOwnerKnowlege(String collectionid, String inUserId)
	{
		Collection<String> docids = getAssistantManager().findDocIdsForEntity("librarycollection", collectionid);

		Data ownerprofile = getAssistantManager().getEmeProfileForUser(inUserId);
		if (ownerprofile != null)
		{
			Collection<String> profiledocids = getAssistantManager().findDocIdsForEntity("emeprofile", ownerprofile.getId());
			docids.addAll(profiledocids);
		}
		return docids;
	}

	protected Collection<String> loadOwnerKnowlege(String inUserId)
	{
		Collection<String> docids = new ArrayList<>();

		Data ownerprofile = getAssistantManager().getEmeProfileForUser(inUserId);
		if (ownerprofile != null)
		{
			Collection<String> profiledocids = getAssistantManager().findDocIdsForEntity("emeprofile", ownerprofile.getId());
			docids.addAll(profiledocids);
		}
		return docids;
	}

}
