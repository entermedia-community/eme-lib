package org.entermediadb.ai.skills;

import java.util.Collection;
import java.util.Iterator;

import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.creator.AiSmartCreatorSteps;
import org.entermediadb.ai.AgentContext;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;

public class SmartCreatorProcessPendingSkill extends BaseSkill
{

	public ChatSmartCreatorConfirmationSkill getSmartCreatorSkill()
	{
		ChatSmartCreatorConfirmationSkill smartCreatorManager = (ChatSmartCreatorConfirmationSkill) getMediaArchive().getBean("smartCreatorSkill");
		return smartCreatorManager;
	}

	@Override
	public void process(AgentContext inContext)
	{

		Collection<String> values = inContext.getCurrentAgentEnable().getAutomationStepData().getValues("searchtypes");
		if (values == null)
		{
			inContext.error("No Agents Enabled");
			return;
		}
		for (Iterator iterator1 = values.iterator(); iterator1.hasNext();)
		{
			String moduleid = (String) iterator1.next();

			MultiValued module = (MultiValued) getMediaArchive().getCachedData("module", moduleid);

			if (module == null)
			{
				// inContext.error("No module found " +
				// inContext.getCurrentAgentEnable().getAutomationStepData());
				continue;
			}
			// Find pending then process each one
			Collection found = getMediaArchive().query(module.getId()).exact("processingstatus", "new").search();
			// Then save?

			String llmprompt = inContext.getCurrentAgentEnable().getAutomationStepData().get("llmprompt");

			inContext.info("Found " + found.size() + " records in " + inContext.getCurrentAgentEnable().getAutomationStepData());

			for (Iterator iterator = found.iterator(); iterator.hasNext();)
			{
				long startTime = System.currentTimeMillis();
				MultiValued entity = (MultiValued) iterator.next();
				inContext.setCurrentEntityModule(module);
				inContext.setCurrentEntity(entity);

				inContext.put("data", entity);
				llmprompt = getMediaArchive().getReplacer().replace(llmprompt, inContext.getContext());

				inContext.info("Processing: " + llmprompt);

				AiSmartCreatorSteps instructions = new AiSmartCreatorSteps(); // Fresh
				instructions.setTargetModule(module);
				instructions.setTargetEntity(entity);

				inContext.setAiSmartCreatorSteps(instructions);

				// getSmartCreatorSkill().parseCreationPrompt(inContext, llmprompt);

				super.process(inContext); // To Create outline

				entity.setValue("processingstatus", "complete");
				getMediaArchive().saveData(module.getId(), entity);
				long duration = (System.currentTimeMillis() - startTime) / 1000L;
				inContext.info("Finished processing in " + duration + "s: " + entity.getName());
			}
		}
	}
}
