package org.entermediadb.ai.skills;

import java.util.Map;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.llm.AgentEnabled;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;

public class HandleWebEventSkill extends BaseSkill
{
	@Override
	public void process(AgentContext inContext)
	{
		// String runoperation =
		// inContext.getCurrentAgentEnable().getAutomationEnabledData().get("runoperation");
		WebPageRequest request = (WebPageRequest) inContext.getContextValue("webpagerequest");

		if (request != null)
		{
			Map params = request.getParameterMap();
			// Map values = getRequestUtils().extractValueMap(request);
			inContext.putContextValues(params);

			if (inContext.getCurrentEntityModule() == null)
			{
				String entityid = request.getRequestParameter("entityid");
				String entitymoduleid = request.getRequestParameter("entitymoduleid");
				if (entitymoduleid != null)
				{
					MultiValued entity = (MultiValued) getMediaArchive().getCachedData(entitymoduleid, entityid);
					inContext.setCurrentEntity(entity);
					MultiValued entitymodule = (MultiValued) getMediaArchive().getCachedData("module", entitymoduleid);
					inContext.setCurrentEntityModule(entitymodule);
				}
			}

			inContext.put("triggersiteroot", request.getSiteRoot());

			inContext.setUserProfile(request.getUserProfile());

			request.putPageValue("currentagentcontext", inContext);

			String nextSkillEnabledId = request.findValue("nextskillenabledid");
			if (nextSkillEnabledId != null)
			{
				inContext.put("nextskillenabledid", nextSkillEnabledId);
				AgentEnabled currentAgentEnabled = inContext.getCurrentScenario().findEnabled(nextSkillEnabledId);
				inContext.setCurrentAgentEnable(currentAgentEnabled);
				currentAgentEnabled.getAgent().process(inContext);
				return;
			}
		}
		super.process(inContext);
	}
}
