package org.entermediadb.ai.agentjobs;

import java.util.ArrayList;
import java.util.List;

import org.openedit.MultiValued;
import org.openedit.data.BaseData;

public class AgentJob extends BaseData 
{
    List<MultiValued> steps = new ArrayList<MultiValued>();
	public void addStep(MultiValued step)
	{
		steps.add(step);
	}

    public List<MultiValued> getSteps()
	{
		return steps;
	}

	public void setSteps(List<MultiValued> inSteps)
	{
		steps = inSteps;
	}
}
