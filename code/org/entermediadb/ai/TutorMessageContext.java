package org.entermediadb.ai;

public class TutorMessageContext extends ChatMessageContext
{

	public TutorMessageContext(AgentContext inContext) {
		super(inContext);
	}

	public TutorMessageContext() {
		// TODO Auto-generated constructor stub
	}

	public void setTutorialId(String inTutorialId)
	{
		setMessageAgentContext("tutorialid", inTutorialId);
	}

	public String getTutorialId()
	{
		return (String) getMessageAgentContext("tutorialid");
	}
}
