package org.entermediadb.ai.skills;

import java.util.Date;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.TutorMessageContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.openedit.Data;
import org.openedit.MultiValued;

public class AdaptiveTutorialEndSkill extends AdaptiveTutorialBaseSkill
{
	@Override
	public void process(AgentContext inAgentContext)
	{
		TutorMessageContext tutorMessageContext = (TutorMessageContext) inAgentContext;

		String tutorialid = (String) tutorMessageContext.getContextValue("tutorialid");

		MultiValued newMessage = (MultiValued) getMediaArchive().getSearcher("chatterbox").createNewData();
		newMessage.setValue("date", new Date());
		newMessage.setValue("channel", tutorMessageContext.getChannel().getId());
		newMessage.setValue("user", "agent");
		newMessage.setValue("messagetype", "system");
		tutorMessageContext.setAgentMessage(newMessage);

		tutorMessageContext.putContextValue("messagerendertype", "end");
		tutorMessageContext.putContextValue("tutorialid", tutorialid);
		tutorMessageContext.putContextValue("sectionid", null);
		tutorMessageContext.putContextValue("componentid", null);

		Data channel = tutorMessageContext.getChannel();
		channel.setValue("channelstatus", "finished");
		getMediaArchive().getSearcher("channel").saveData(channel, null);

		AgentEnabled skillEnabled = tutorMessageContext.getCurrentAgentEnable();
		tutorMessageContext.fireStatusComplete(skillEnabled);
	}
}
