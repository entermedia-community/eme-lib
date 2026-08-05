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
		return (String) getAgentMessage().getJSONValue("agentcontextvalues", "tutorialid");
	}

	public void setLastSectionId(String inSectionId)
	{
		setMessageAgentContext("lastsectionid", inSectionId);
	}

	public String getLastSectionId()
	{
		return (String) getAgentMessage().getJSONValue("agentcontextvalues", "lastsectionid");
	}

	public void setLastComponentId(String inComponentId)
	{
		setMessageAgentContext("lastcomponentid", inComponentId);
	}

	public String getLastComponentId()
	{
		return (String) getAgentMessage().getJSONValue("agentcontextvalues", "lastcomponentid");
	}
}
