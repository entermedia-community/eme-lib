package org.entermediadb.ai.skills;

import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.openedit.MultiValued;

public class SmartCreatorRenderAssetSkill extends BaseSkill
{
	public SmartCreatorPlaybackSkill getSmartCreatorSkill()
	{
		SmartCreatorPlaybackSkill manager = (SmartCreatorPlaybackSkill) getMediaArchive().getBean("smartCreatorPlaybackSkill");
		return manager;
	}

	/**
	 * Calls render to html Attaches and asset version sends it to a blog?
	 */
	@Override
	public void process(AgentContext inContext)
	{
		MultiValued entity = inContext.getCurrentEntity();

		if (entity != null)
		{
			// render html and save to asset
			MultiValued entitymodule = inContext.getCurrentEntityModule();
			String applicationid = getMediaArchive().getCatalogSettingValue("mediadbappid");
			String cdnprefix = (String) inContext.getContextValue("triggersiteroot");
			String html = getSmartCreatorSkill().renderToHtml(cdnprefix, applicationid, entitymodule, entity);
			getSmartCreatorSkill().exportAsAsset(inContext, entitymodule, entity, html);
		}
		super.process(inContext);
	}

}
