package org.entermediadb.ai.skills;

import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.eme.EmeChatContext;

public class EmeChatRespondSkill extends BaseSkill
{

	@Override
	public void process(AgentContext inContext)
	{
		// TODO Auto-generated method stub
		EmeChatContext chatMessageContext = new EmeChatContext(inContext);

		super.process(inContext);
	}

}
