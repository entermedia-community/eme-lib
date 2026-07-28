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
		putContextValue("tutorialid", inTutorialId);
	}

	public String getTutorialId()
	{
		return (String) getContextValue("tutorialid");
	}

	public void setLastSectionId(String inSectionId)
	{
		putContextValue("lastsectionid", inSectionId);
	}

	public String getLastSectionId()
	{
		return (String) getContextValue("lastsectionid");
	}

	public void setLastComponentId(String inComponentId)
	{
		putContextValue("lastcomponentid", inComponentId);
	}

	public String getLastComponentId()
	{
		return (String) getContextValue("lastcomponentid");
	}
}
