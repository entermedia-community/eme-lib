package org.entermediadb.ai.agentjobs;

import org.openedit.Data;
import org.openedit.MultiValued;

public interface AgentJobListener
{

	public void finishedAllSteps(AgentJobRunnable inJob);

	public void finishedStep(AgentJobRunnable inJob, Data inStep);

	public void runStep(AgentJobRunnable agentJobRunnable, MultiValued step);
}
