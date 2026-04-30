package org.openinstitute.community;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.data.QueryBuilder;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.page.PageLoader;
import org.openedit.page.manage.PageManager;
import org.openedit.servlet.RightPage;
import org.openedit.servlet.Site;
import org.openedit.util.PathUtilities;
import org.openedit.util.URLUtilities;

public class ProjectLoader implements PageLoader, CatalogEnabled
{
	protected ModuleManager fieldModuleManager;
	protected PageManager fieldPageManager;
	protected String fieldCatalogId;
	private static final Log log = LogFactory.getLog(ProjectLoader.class);

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	protected MediaArchive getMediaArchive()
	{
		return (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
	}

	@Override
	public RightPage getRightPage(URLUtilities util, Site site, Page inPage)
	{
		if (site == null)
		{
			RightPage right = new RightPage();
			right.setRightPage(inPage);
			return right;

		}
		// if( sitedata != null )
		// {
		// String realpath = sitedata.get("domainpath");
		// //This just adds back the missing /site/..
		// if( !inPage.getPath().startsWith(realpath))
		// {
		// //sitedata.fixRealPath(realpath)
		// Page page = getPageManager().getPage(realpath + inPage.getPath());
		// RightPage right = new RightPage();
		// right.setRightPage(page);
		// return right;
		// }
		// }
		String requestedPath = util.getOriginalPath();
		// Only works with domains being set. Otherwise use normal page actions to load
		// project pages
		String[] url = requestedPath.split("/");
		if (url.length > 1 && (url[1].equals("mediadb")))
		{
			return null;
		}

		// Check that we are actually going to the page /site/community/...
		String appid = inPage.getProperty("applicationid");
		if (appid != null && url.length > 0 && appid.startsWith(url[1]))
		{
			return null;
		}

		// Check domain?
		String domain = util.domain();
		// String[] subdomain = domain.split("\\.");
		// if(subdomain.length < 3)
		// {
		// subdomain = ("default." + domain).split("\\.");
		// }
		// String communityurlname = null;
		String secondpart = null;
		String anythingelse = null;

		// communityurlname = subdomain[0];
		if (url.length > 1) // Might be a virtual project
		{
			secondpart = url[1]; // might be wrong
			if (url.length > 2)
			{
				anythingelse = requestedPath.substring(requestedPath.indexOf(secondpart) + secondpart.length());
			}
		}

		if (secondpart == null)
		{
			RightPage page = goHome(inPage, domain);
			// if( page == null)
			// {
			// String siteid = inPage.get("siteid");
			//
			// Page apphome = getPageManager().getPage("/" + siteid + "/app/index.html");
			// page = new RightPage();
			// page.setRightPage(apphome);
			// }
			return page;
		}

		// TODO: Keep a cached list of sub folders where we always load the page from
		// blogs projects etc and assume .../index.html
		if (getMediaArchive().getCatalogId().endsWith("notset"))
		{
			throw new OpenEditException("Invalid catalog for " + requestedPath + " " + getMediaArchive().getCatalogId());
		}

		// Does the page exist or is it a project?
		String siteid = inPage.get("siteid");
		Data communitydata = findCommunity(domain);
		if (communitydata == null)
		{
			log.info("Couldn't find Community Data: " + domain + " Second part: " + secondpart);
			return null;
		}
		String communityhome = "/" + siteid + communitydata.get("templatepath"); // Use Mask?
		String fixedpath = communityhome + "/" + secondpart;

		Page page = null;

		if (anythingelse == null)
		{
			page = getPageManager().getPage(fixedpath);
		}
		else
		{
			fixedpath = fixedpath + anythingelse;
			page = getPageManager().getPage(fixedpath);
		}

		RightPage right = new RightPage();
		right.putParam("communitytagcategory", communitydata.getId());
		right.putPageValue("communitytagcategory", communitydata);
		right.putPageValue("communitylink", "");
		right.putPageValue("communityhome", communityhome);
		if (page.exists()) // Must be a real page
		{
			if (page.isFolder())
			{
				page = getPageManager().getPage(fixedpath + "/index.html");
			}
			right.setRightPage(page);
			return right;
		}
		else
		{
			Page indexpage = getPageManager().getPage(fixedpath + "/index.html");
			if (indexpage.exists()) // Must be a real page
			{
				right.setRightPage(indexpage);
				return right;
			}
			if (Boolean.parseBoolean(page.get("virtual")))
			{
				right.setRightPage(page);
				return right;
			}
		}

		// Must be a project with something on the end?
		QueryBuilder query = getMediaArchive().query("librarycollection").exact("urlname", secondpart).hitsPerPage(1);
		HitTracker hits = getMediaArchive().getCachedSearch(query);
		Data librarycollection = (Data) hits.first();
		if (librarycollection != null)
		{
			String template = null;
			if (anythingelse == null)
			{
				template = communityhome + "/project/blog-list/index.html";
			}
			else
			{
				template = communityhome + "/project" + anythingelse;
			}
			String justname = PathUtilities.extractFileName(template);
			if (!justname.contains("."))
			{
				template = template + "/index.html";
			}
			Page otherpage = getPageManager().getPage(template);
			// if( !otherpage.exists())
			// {
			// //log.info("Cant find " + template);
			// }
			right.putParam("collectionid", librarycollection.getId());
			librarycollection = getMediaArchive().getSearcher("librarycollection").loadData(librarycollection);
			right.putPageValue("librarycol", librarycollection);
			// right.putPageValue("projectlink" , "/" + domain + "/" +
			// librarycollection.get("urlname") );
			right.setRightPage(otherpage);
			return right;
		}
		else
		{
			// log.info("Couldn't find Collection: " + secondpart);
		}
		if (log.isDebugEnabled())
			log.debug("Couldn't find any content: Orinal path: " + requestedPath + " Community Home:" + communityhome);
		return null;
	}

	protected RightPage goHome(Page inPage, String domain)
	{
		Data first = findCommunity(domain);
		String siteid = inPage.get("siteid");

		if (first != null)
		{
			if (first.get("templatepath") == null)
			{
				throw new OpenEditException("templatepath is required for " + domain);
			}
			String communityhome = "/" + siteid + first.get("templatepath");

			String template = communityhome + "/index.html"; // ?communitytagcategoryid=" + first.getId()
																// communities/emedia/home.html
			Page page = getPageManager().getPage(template);
			RightPage right = new RightPage();
			right.putParam("communitytagcategory", first.getId());
			right.putPageValue("communitytagcategory", first);
			right.putPageValue("communityhome", communityhome);
			log.info("Set communityhome=" + communityhome);

			right.setRightPage(page);
			return right;

		}
		return null;
	}

	protected Data findCommunity(String domain)
	{
		QueryBuilder query = getMediaArchive().query("communitytagcategory").match("domainlist", domain).hitsPerPage(1); // Move
																															// to
																															// use
																															// the
																															// domain

		HitTracker hits = getMediaArchive().getCachedSearch(query);
		if (hits.isEmpty())
		{
			log.info("Not Found community:" + hits + " for " + domain + " in " + getMediaArchive().getCatalogId());
		}
		Data first = (Data) hits.first();
		return first;
	}

	@Override
	public void setCatalogId(String inId)
	{
		fieldCatalogId = inId;

	}

	protected String getCatalogId()
	{
		return fieldCatalogId;
	}

}
