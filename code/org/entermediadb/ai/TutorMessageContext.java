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

	public void setLastSectionId(String inSectionId)
	{
		setMessageAgentContext("sectionid", inSectionId);
	}

	public String getLastSectionId()
	{
		return (String) getMessageAgentContext("sectionid");
	}

	public void setLastComponentId(String inComponentId)
	{
		setMessageAgentContext("componentid", inComponentId);
	}

	public String getLastComponentId()
	{
		return (String) getMessageAgentContext("componentid");
	}
}
