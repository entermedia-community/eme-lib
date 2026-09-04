package org.entermediadb.ai.skills;

import java.util.ArrayList;
import java.util.Collection;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.creator.AiSmartCreatorSteps;
import org.entermediadb.ai.llm.AutomationStep;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.Data;
import org.openedit.MultiValued;

public class SmartCreatorMakeSuggestionsSkill extends BaseSkill
{
	@Override
	public void process(AgentContext inContext)
	{
		makeSuggestions(inContext, inContext.getAiSmartCreatorSteps());
		// super.process(inContext);
	}

	public void makeSuggestions(AgentContext messageContext, AiSmartCreatorSteps instructions)
	{

		// This was passed in from html data-context_playbackentitymoduleid="$module.getId()"
		String playbackentitymoduleid = (String) messageContext.getContextValue("playbackentitymoduleid");
		Data playbackentitymodule = getMediaArchive().getCachedData("module", playbackentitymoduleid);
		messageContext.addContext("playbackentitymodule", playbackentitymodule);

		instructions = new AiSmartCreatorSteps();
		instructions.setTargetModule(playbackentitymodule);
		messageContext.setAiSmartCreatorSteps(instructions);

		String entityid = messageContext.get("entityid");
		String entitymoduleid = messageContext.get("entitymoduleid");

		MultiValued entity = (MultiValued) getMediaArchive().getCachedData(entitymoduleid, entityid);
		messageContext.addContext("entity", entity);

		Collection<String> semantictopics = entity.getValues("semantictopics");
		if (semantictopics == null || semantictopics.isEmpty())
		{
			// pull semantic topics from entityassets views
			Collection<Data> entityassets = getMediaArchive().getSearcher("entityasset").query().exact(entitymoduleid, entityid).search();

			semantictopics = new ArrayList<String>();

			for (Data entityasset : entityassets)
			{
				Collection<String> entityassettopics = entityasset.getValues("semantictopics");
				if (entityassettopics == null || entityassettopics.isEmpty())
				{
					continue;
				}

				semantictopics.addAll(entityassettopics);

				if (semantictopics.size() >= 5)
				{
					break;
				}
			}
		}

		messageContext.addContext("semantictopics", semantictopics);

		messageContext.setCurrentEntity(entity);

		MultiValued module = (MultiValued) getMediaArchive().getCachedData("module", entitymoduleid);
		messageContext.setCurrentEntityModule(module);

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("thinking");
		LlmResponse response = llmconnection.renderLocalAction(messageContext, "chat_smartcreator_suggest");
		messageContext.setWaitTime(null);
		// This is for the chat UI to pass it back
		// response.setNextSkillEnabled("smartcreator_parse"); chat_smartcreator_parse_user_prompt
		messageContext.setLastResponse(response);

		AutomationStep skillEnabled = messageContext.getCurrentAgentEnable();
		messageContext.fireStatusComplete(skillEnabled);
		return;
	}

}
