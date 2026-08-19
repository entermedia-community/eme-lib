package org.entermediadb.ai.skills;

import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.TutorMessageContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.openedit.Data;

public class AdaptiveTutorialEndSkill extends AdaptiveTutorialBaseSkill
{
	@Override
	public void process(AgentContext inAgentContext)
	{
		TutorMessageContext tutorMessageContext = (TutorMessageContext) inAgentContext;

		String tutorialid = (String) tutorMessageContext.getContextValue("tutorialid");

		Data agentmessage = tutorMessageContext.getAgentMessage();
		agentmessage.setValue("messagetype", "system");

		tutorMessageContext.setMessageAgentContext("messagetype", "end");
		tutorMessageContext.setMessageAgentContext("tutorialid", tutorialid);
		tutorMessageContext.setMessageAgentContext("sectionid", null);
		tutorMessageContext.setMessageAgentContext("componentid", null);

		AgentEnabled skillEnabled = tutorMessageContext.getCurrentAgentEnable();
		tutorMessageContext.fireStatusComplete(skillEnabled);
	}
}
