package org.entermediadb.ai.informatics;

import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.llm.BaseAgentContext;
import org.entermediadb.asset.Asset;
import org.openedit.MultiValued;

public class InformaticsContext extends BaseAgentContext
{
	private static final Log log = LogFactory.getLog(InformaticsContext.class);

	public InformaticsContext(AgentContext inContext) {
		super(inContext);
	}

	public InformaticsContext() {
		// TODO Auto-generated constructor stub
	}

	public InformaticsContext getParentInformaticContext()
	{
		if (fieldParentContext != null && fieldParentContext instanceof InformaticsContext)
		{
			return (InformaticsContext) fieldParentContext;
		}
		return null;
	}

	public Collection<MultiValued> getRecordsToProcess()
	{

		Collection<MultiValued> records = (Collection<MultiValued>) getContextValue("recordsToProcess");
		return records;
	}

	public void setRecordsToProcess(Collection<MultiValued> inRecordsToProcess)
	{
		putContextValue("recordsToProcess", inRecordsToProcess);
	}

	public Collection<Asset> getAssetsToProcess()
	{
		Collection<Asset> assets = (Collection<Asset>) getContextValue("assetsToProcess");
		return assets;
	}

	public void setAssetsToProcess(Collection<Asset> inAssetsToProcess)
	{
		putContextValue("assetsToProcess", inAssetsToProcess);
	}

}
