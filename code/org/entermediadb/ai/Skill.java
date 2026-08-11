package org.entermediadb.ai;

public interface Skill
{
	public void startupScenario(AgentContext inContext);

	public void endScenario(AgentContext inContext);

	void process(AgentContext inContext);

}
