package org.entermediadb.ai.skills;

import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.classify.EmbeddingManager;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.MultiValued;
import org.openedit.hittracker.HitTracker;

public class AgentJobCreatorSkill extends BaseSkill
{
    private static final Log log = LogFactory.getLog(AgentJobCreatorSkill.class);

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;

		//Needed?
		messageContext.fireStatusStarting(messageContext.getCurrentAgentEnable());

		MultiValued agentmessage = messageContext.getAgentMessage();
		MultiValued usermessage = (MultiValued) getMediaArchive().getCachedData("chatterbox", agentmessage.get("replytoid"));

		Collection<String> docids = loadSkillDocIds();
		EmbeddingManager embeddings = (EmbeddingManager) getMediaArchive().getBean("embeddingManager");
		LlmResponse response = embeddings.callStructure(messageContext, docids, "","");

		String responseText = response.getMessage();

		//Parse this as JSON?
		

		//messageContext.putContextValue("goal", goal);
		super.process(messageContext);

		// AutomationStep skillEnabled = messageContext.getCurrentAgentEnable();
		// fireStatusComplete(skillEnabled);

		// getMediaArchive().fireSharedMediaEvent("goaltask/goalcreated");

		return;
	}

	protected Collection<String> loadSkillDocIds()
	{
		Collection<String> ids = (Collection<String> )getMediaArchive().getCacheManager().get("skills","skillids");
		if(ids == null)
		{
			HitTracker skillds = getMediaArchive().getList("agentskill");
			ids = skillds.collectValues("id");

			HitTracker automations = getMediaArchive().getList("automationscenario");
			Collection<String> moreids = automations.collectValues("id");
			ids.addAll(moreids);	
			getMediaArchive().getCacheManager().put("skills","skillids", ids);
		}
		return ids; // Replace with actual collection of document IDs
	}
	
}
