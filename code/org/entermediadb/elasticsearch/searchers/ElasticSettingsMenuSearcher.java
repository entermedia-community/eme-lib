package org.entermediadb.elasticsearch.searchers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.ValuesMap;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.hittracker.Term;
import org.openedit.users.User;

public class ElasticSettingsMenuSearcher extends ElasticListSearcher
{

	@Override
	public boolean hasChanged(HitTracker inTracker)
	{
		boolean changed = super.hasChanged(inTracker);
		return changed;
	}

	public void saveData(Data inData, User inUser)
	{
		super.saveData(inData, inUser);
	}

	@Override
	public void saveAllData(Collection<Data> inAll, User inUser)
	{
		super.saveAllData(inAll, inUser);
	}

	@Override
	public void delete(Data inData, User inUser)
	{
		// Mark as deleted to hide the parents from showing up?
		inData.setValue("deleted", true);
		saveData(inData);
		// super.delete(inData, inUser);
	}

	@Override
	public HitTracker search(SearchQuery inSearch) throws OpenEditException
	{
		HitTracker actualviews = super.search(inSearch);

		Term moduleid = inSearch.getTermByDetailId("moduleid");
		if (moduleid == null)
		{
			return actualviews;
		}

		if (moduleid.getValue().equals("system"))
		{
			return actualviews;
		}
		// filter by moduleid

		Collection alltemplaterows = getSearcherManager().getList(getCatalogId(), "menumoduletemplate");
		HitTracker combinedviews = mergeResults(actualviews, moduleid.getValue(), alltemplaterows);

		List<MultiValued> finallist = new ArrayList<MultiValued>();

		Map<String, String> filters = new HashMap();

		Term parentidTerm = inSearch.getTermByDetailId("parentid");
		if (parentidTerm != null)
		{
			filters.put("parentid", parentidTerm.getValue());
		}
		Term toplevelTerm = inSearch.getTermByDetailId("menulevel");
		if (toplevelTerm != null)
		{
			filters.put("menulevel", toplevelTerm.getValue());
		}

		// Load all data to make sure we have the right types and not SearchHitData
		for (Iterator iterator = combinedviews.iterator(); iterator.hasNext();)
		{
			Data d = (Data) iterator.next();
			// Make sure that matches what they asked for
			boolean skip = false;
			for (Map.Entry<String, String> entry : filters.entrySet())
			{
				String key = entry.getKey();
				String value = entry.getValue();
				String fieldvalue = d.get(key);
				if (fieldvalue == null || !value.equals(fieldvalue))
				{
					skip = true;
					break;
				}
			}
			if (skip)
			{
				continue;
			}
			MultiValued data = (MultiValued) loadData(d);
			finallist.add(data);
		}

		Collections.sort(finallist, new Comparator<MultiValued>() {
			@Override
			public int compare(MultiValued inO1, MultiValued inO2)
			{
				long i1 = inO1.getLong("ordering");
				long i2 = inO2.getLong("ordering");
				if (i1 == i2)
				{
					return 0;
				}
				if (i1 < i2)
				{
					return -1;
				}
				else
				{
					return 1;
				}
			}
		});
		ListHitTracker combined = new ListHitTracker(finallist);
		combined.setIndexId(getIndexId());
		return combined;
	}

	@Override
	public Object searchById(String inId)
	{
		if (inId == null)
		{
			return null;
		}
		// Make sure we call getCachedData for views
		MultiValued data = (MultiValued) super.searchById(inId);

		if (data != null)
		{
			if (data.getBoolean("deleted"))
			{
				return null;
			}
		}

		// Could be a virtual record
		if (data == null) // TODO: Cache lookup?
		{
			HitTracker hits = getSearcherManager().query(getCatalogId(), "menumoduletemplate").all().cachedSearch();
			for (Iterator iterator = hits.iterator(); iterator.hasNext();)
			{
				Data hit = (Data) iterator.next();

				// Look for a matching ending. The template id is the ending of the real id
				String id = hit.getId().replace("MODULEID", "");
				if (inId.endsWith(id))
				{
					// Cut off the common ending and use the rest as the moduleid
					String moduleid = inId.substring(0, inId.length() - id.length());
					MultiValued MultiValued = loadNewData(moduleid, hit);
					return MultiValued;
				}
			}
		}
		return data;
	}

	// TODO Save deleted with special flag

	protected HitTracker mergeResults(HitTracker actualviews, String inModuleId, Collection baseresults)
	{
		ListHitTracker combinedviews = new ListHitTracker();
		Set deletedViews = new HashSet();

		for (Iterator iterator = actualviews.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			MultiValued existing = (MultiValued) loadData(hit);
			if (existing.getBoolean("deleted"))
			{
				deletedViews.add(existing.getId());
				continue;
			}
			combinedviews.add(existing);
		}
		// Fix all the IDS and parents and module
		for (Iterator iterator = baseresults.iterator(); iterator.hasNext();)
		{
			Data template = (Data) iterator.next();
			MultiValued fixedtemplateMultiValued = loadNewData(inModuleId, template);
			if (deletedViews.contains(fixedtemplateMultiValued.getId()))
			{
				continue;
			}
			MultiValued alreadyadded = (MultiValued) combinedviews.findData("id", fixedtemplateMultiValued.getId());
			if (alreadyadded == null)
			{
				combinedviews.add(fixedtemplateMultiValued); // From the template area
			}
		}

		// Resort
		return combinedviews;
	}

	protected MultiValued loadNewData(String inModuleId, Data inTemplateView)
	{
		String id = inTemplateView.getId();
		id = id.replace("MODULEID", toId(inModuleId));

		MultiValued data = (MultiValued) createNewData();
		ValuesMap fields = inTemplateView.getProperties();
		fields = checkTypes(fields);
		data.setProperties(fields);
		data.setId(id);
		data.setValue("moduleid", inModuleId);

		fixField(data, "parentid", inModuleId);
		fixField(data, "path", inModuleId);
		return data;
	}

	protected void fixField(MultiValued data, String inField, String inModuleId)
	{
		String value = data.get(inField);
		if (value != null)
		{
			value = value.replace("MODULEID", toId(inModuleId));
			data.setValue(inField, value);
		}
	}

}
