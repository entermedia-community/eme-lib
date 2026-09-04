package org.entermediadb.ai;

import org.entermediadb.ai.llm.AutomationStep;

public interface SkillStatusListener
{
	void handleStatusStarting(AgentContext inContext, AutomationStep inAutomationStep);

	void handleStatusComplete(AgentContext inContext, AutomationStep inAutomationStep);
}
