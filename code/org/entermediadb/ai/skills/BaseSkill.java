package org.entermediadb.ai;

import java.util.Collection;
import org.entermediadb.ai.llm.AgentEnabled;
import org.openedit.CatalogEnabled;

public class BaseSkill extends BaseAiManager implements Skill, CatalogEnabled
{
	public void startupScenario(AgentContext inContext)
	{
		// fire websocket event
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

}