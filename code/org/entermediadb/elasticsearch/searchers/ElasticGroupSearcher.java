/*
 * Created on Oct 19, 2004
 */
package org.entermediadb.elasticsearch.searchers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.users.Group;
import org.openedit.users.GroupSearcher;

/**
 * @author cburkey
 * 
 */
public class ElasticGroupSearcher extends BaseElasticSearcher implements GroupSearcher
{
	private static final Log log = LogFactory.getLog(ElasticGroupSearcher.class);

	public Group getGroup(String inGroupId)
	{
		Data lookup = getSearcherManager().getCachedData(getCatalogId(), getSearchType(), inGroupId);
		Group group = (Group) lookup; // Should be loaded already
		return group;
	}

}
