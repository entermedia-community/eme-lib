package org.entermediadb.manager;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.asset.util.CSVReader;
import org.entermediadb.asset.util.ImportFile;
import org.entermediadb.asset.util.Row;
import org.entermediadb.elasticsearch.ElasticNodeManager;
import org.entermediadb.elasticsearch.searchers.ElasticListSearcher;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.NonExportable;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.modules.translations.LanguageMap;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.FileUtils;
import org.openedit.util.PathUtilities;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public class SiteSnapshotManager extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(SiteSnapshotManager.class);

	public void restoreSnapshot(WebPageRequest inReq, Data snapshot) throws Exception
	{
		log.info("Initializing restore");
		SearcherManager searcherManager = (SearcherManager) inReq.getPageValue("searcherManager");
		Searcher snapshotsearcher = searcherManager.getSearcher("system", "sitesnapshot");

		snapshot.setValue("snapshotstatus", "restoring");
		snapshotsearcher.saveData(snapshot);

		Searcher sitesearcher = searcherManager.getSearcher("system", "site");
		Data site = sitesearcher.query().match("id", snapshot.get("site")).searchOne();

		String catalogid = site.get("catalogid");
		MediaArchive mediaarchive = (MediaArchive) getModuleManager().getBean(catalogid, "mediaArchive");

		try
		{
			Boolean configonly = (Boolean) snapshot.getValue("configonly");
			if (configonly == null)
			{
				configonly = false;
			}

			String logstring = String.format("restoring: %s config= %s ", site.get("rootpath"), configonly);
			log.info(logstring);
			ScriptLogger scriptLogger = (ScriptLogger) inReq.getPageValue("log");
			restore(scriptLogger, mediaarchive, site, snapshot, configonly);
			snapshot.setValue("snapshotstatus", "complete");
		}
		catch (Exception ex)
		{
			ScriptLogger scriptLogger = (ScriptLogger) inReq.getPageValue("log");
			if (scriptLogger != null)
			{
				scriptLogger.error("Could not restore " + ex.getMessage(), ex);
			}
			else
			{
				log.error("Could not restore", ex);
			}
			snapshot.setValue("snapshotstatus", "error");
		}
		finally
		{
			mediaarchive.getSearcherManager().resetAlternative();
		}
		snapshotsearcher.saveData(snapshot);
		mediaarchive.getSearcherManager().clear();

		log.info("Snapshot restore finished.");

	}

	public void restore(ScriptLogger scriptLogger, MediaArchive mediaarchive, Data site, Data inSnap, boolean configonly) throws Exception
	{
		String folder = inSnap.get("folder");
		String catalogid = mediaarchive.getCatalogId();
		String rootfolder = "/WEB-INF/data/exports/" + mediaarchive.getCatalogId() + "/" + folder;

		@SuppressWarnings("unchecked")
		Collection<String> files = mediaarchive.getPageManager().getChildrenPaths(rootfolder);
		if (files.isEmpty())
		{
			throw new OpenEditException("No files in " + rootfolder);
		}

		Date date = new Date();
		ElasticNodeManager nodeManager = (ElasticNodeManager) mediaarchive.getNodeManager();

		String tempindex = nodeManager.toId(mediaarchive.getCatalogId().replaceAll("_", "") + date.getTime());

		Page lists = mediaarchive.getPageManager().getPage(rootfolder + "/lists/");
		if (lists.exists())
		{
			Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/lists/");
			archiveFolder(mediaarchive.getPageManager(), target, tempindex);
			mediaarchive.getPageManager().copyPage(lists, target);
		}

		Page views = mediaarchive.getPageManager().getPage(rootfolder + "/views/");
		if (views.exists())
		{
			Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/views/");
			archiveFolder(mediaarchive.getPageManager(), target, tempindex);
			mediaarchive.getPageManager().copyPage(views, target);
		}

		Page orig = mediaarchive.getPageManager().getPage(rootfolder + "/originals");
		if (orig.exists())
		{
			Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/originals/");
			mediaarchive.getPageManager().copyPage(orig, target);
		}

		Page gen = mediaarchive.getPageManager().getPage(rootfolder + "/generated");
		if (gen.exists())
		{
			Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/generated/");
			mediaarchive.getPageManager().copyPage(gen, target);
		}

		mediaarchive.getPageManager().clearCache();
		scriptLogger.info("Clearing property definitions");
		PropertyDetailsArchive pdarchive = mediaarchive.getPropertyDetailsArchive();

		Page fields = mediaarchive.getPageManager().getPage(rootfolder + "/fields/");
		if (fields.exists())
		{
			Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/fields/");
			archiveFolder(mediaarchive.getPageManager(), target, tempindex);
			mediaarchive.getPageManager().copyPage(fields, target);
		}
		pdarchive.clearCache();

		List<String> jsonfiles = pdarchive.getPageManager().getChildrenPaths(rootfolder + "/json/");

		if (!configonly)
		{
			if (jsonfiles.size() < 300)
			{
				throw new OpenEditException("Not enough json files found in " + rootfolder + "/json/ only found " + jsonfiles.size());

			}
			scriptLogger.info("Preparing index " + tempindex);
			nodeManager.prepareIndex(tempindex);
			scriptLogger.info("Index " + tempindex + " prepared");
		}

		List<String> orderedtypes = new ArrayList<>();
		orderedtypes.add("category");

		List<String> childrennames = pdarchive.findChildTablesNames();

		@SuppressWarnings("unchecked")

		List<String> mappings = new ArrayList<>();
		List<String> orderedJsontypes = new ArrayList<>();

		for (String it : jsonfiles)
		{
			if (it.endsWith(".zip"))
			{
				String searchtype = PathUtilities.extractPageName(it);
				if (!childrennames.contains(searchtype))
				{
					orderedJsontypes.add(searchtype);
				}
			}
			if (it.endsWith(".json"))
			{
				String filename = PathUtilities.extractPageName(it);
				mappings.add(filename);
			}
		}

		orderedJsontypes.addAll(childrennames);
		orderedJsontypes.remove("propertydetail");
		orderedJsontypes.remove("lock");
		orderedJsontypes.remove("user");
		orderedJsontypes.remove("group");
		String databaseIndex = tempindex;
		if (configonly)
		{
			databaseIndex = nodeManager.getIndexNameFromAliasName(mediaarchive.getCatalogId().replaceAll("/", "_"));
		}
		for (String it : mappings)
		{
			Page upload = mediaarchive.getPageManager().getPage(rootfolder + "/json/" + it + ".json");
			String searchtype = it.substring(0, it.indexOf("-"));
			scriptLogger.info("Restore - Put Mappings: " + searchtype);

			putMapping(mediaarchive, searchtype, upload, databaseIndex);
		}

		/*
		 * Searcher categories = mediaarchive.getSearcher("category");
		 * categories.setAlternativeIndex(tempindex); log.info("Restore - Put Mappings: category");
		 * categories.putMappings(); categories.setAlternativeIndex(null);
		 */
		scriptLogger.info("Importing Data for " + orderedJsontypes.size() + " types");
		scriptLogger.info(orderedJsontypes);
		for (String type : orderedJsontypes)
		{
			Page upload = mediaarchive.getPageManager().getPage(rootfolder + "/json/" + type + ".zip");
			try
			{
				if (upload.exists())
				{
					scriptLogger.info("Restore - Importing: " + type);
					importJson(site, mediaarchive, type, upload, databaseIndex);
				}
			}
			catch (Exception e)
			{
				scriptLogger.error("Exception thrown importing upload: " + upload, e);
				break;
			}

		}
		if (!configonly)
		{
			scriptLogger.info("Switching alias " + catalogid + " to " + databaseIndex);
			nodeManager.loadIndex(catalogid, databaseIndex, true);
		}
		scriptLogger.info("Import Data completed");

	}

	public void archiveFolder(PageManager inManager, Page inPage, String inIndex)
	{
		if (inPage.exists() && !"false".equals(inPage.get("cleanonimport")))
		{
			Page trash = inManager.getPage("/WEB-INF/trash/" + inIndex + inPage.getPath());
			inManager.movePage(inPage, trash);
		}
	}

	@SuppressWarnings("unchecked")
	public void importCsv(Data site, MediaArchive mediaarchive, String searchtype, Page upload, String tempindex) throws Exception
	{
		Boolean fastmode = Boolean.parseBoolean(mediaarchive.getPageManager().getPage("/WEB-INF/data/system/configuration/testimportmode.xml").get("testimportmode"));

		log.info("Importing data " + upload.getPath());
		Row trow = null;
		ArrayList<Data> tosave = new ArrayList<>();

		Reader reader = upload.getReader();
		ImportFile file = new ImportFile();
		file.setParser(new CSVReader(reader, ',', '\"'));
		file.read(reader);

		PropertyDetailsArchive pdarchive = mediaarchive.getPropertyDetailsArchive();
		PropertyDetails details = pdarchive.getPropertyDetails(searchtype);

		Searcher searcher = mediaarchive.getSearcher(searchtype);
		details = searcher.getPropertyDetails();
		searcher.setAlternativeIndex(tempindex);
		if (!searcher.putMappings())
		{
			throw new OpenEditException("Could not define dynamic or static fields, check mapping errors");
		}

		int count = 0;
		searcher.setForceBulk(true);
		while ((trow = file.getNextRow()) != null && ((fastmode && count < 1000) || !fastmode))
		{
			count++;
			String id = trow.get("id");
			Data newdata = searcher.createNewData();
			newdata.setId(id);

			for (Object headerObj : file.getHeader().getHeaderNames())
			{
				String header = String.valueOf(headerObj);
				String detailid = header;
				String value = trow.get(header);

				if (detailid != null && detailid.contains("."))
				{
					continue;
				}

				PropertyDetail detail = details.getDetail(detailid);
				if (detail == null)
				{
					for (PropertyDetail pd : details)
					{
						String legacy = pd.get("legacy");
						if (legacy != null && legacy.equals(header))
						{
							detail = pd;
							break;
						}
					}
				}

				if (header.contains("."))
				{
					String[] splits = header.split("\\.");
					if (splits.length > 1)
					{
						detail = searcher.getDetail(splits[0]);
						if (detail != null && detail.isMultiLanguage())
						{
							LanguageMap map = null;
							Object values = newdata.getValue(detail.getId());
							if (values instanceof LanguageMap)
							{
								map = (LanguageMap) values;
							}
							if (values instanceof String)
							{
								map = new LanguageMap();
								map.put("en", (String) values);
							}
							if (map == null)
							{
								map = new LanguageMap();
							}
							map.put(splits[1], value);
							newdata.setValue(detail.getId(), map);
						}
					}
					continue;
				}

				if (detail == null)
				{
					continue;
				}
				if (value == null)
				{
					continue;
				}

				if (detail.isDate())
				{
					try
					{
						Date date = DateStorageUtil.getStorageUtil().parseFromStorage(value);
						newdata.setValue(detail.getId(), date);
					}
					catch (Exception e)
					{
						log.error("Parse issue " + value);
					}
				}
				else
				{
					if ("app".equals(searchtype) && "deploypath".equals(detail.getId()))
					{
						int inx = value.indexOf("/");
						if (inx > 1)
						{
							value = site.get("rootpath") + value.substring(inx - 1);
						}
					}
					newdata.setValue(detail.getId(), value);
				}
			}

			tosave.add(newdata);

			if (tosave.size() > 10000)
			{
				searcher.saveAllData(tosave, null);
				tosave.clear();
			}
		}

		searcher.saveAllData(tosave, null);
		searcher.setAlternativeIndex(null);
		FileUtils.safeClose(reader);
		searcher.setForceBulk(false);
		searcher.setAlternativeIndex(null);
		searcher.clearIndex();
		log.info("Saved " + searchtype + " " + tosave.size());
	}

	public void importJson(Data site, MediaArchive mediaarchive, String searchtype, Page upload, String tempindex) throws Exception
	{
		Searcher searcher = mediaarchive.getSearcher(searchtype);
		// if (searcher instanceof ElasticListSearcher)
		// {
		// return;
		// }

		ElasticNodeManager manager = (ElasticNodeManager) mediaarchive.getNodeManager();
		BulkProcessor processor = manager.getBulkProcessor();
		int count = 0;
		try
		{
			ZipInputStream unzip = new ZipInputStream(upload.getInputStream());
			if (unzip.getNextEntry() == null)
			{
				log.info("No zip entries found in " + upload.getPath());
				return;
			}

			MappingJsonFactory f = new MappingJsonFactory();
			JsonParser jp = f.createParser(new InputStreamReader(unzip, "UTF-8"));

			JsonToken current;
			current = jp.nextToken();
			if (current != JsonToken.START_OBJECT)
			{
				log.info("Error: root should be object: quiting.");
				return;
			}

			while (jp.nextToken() != JsonToken.END_OBJECT)
			{
				String fieldName = jp.getCurrentName();
				current = jp.nextToken();
				if (fieldName.equals(searchtype))
				{
					if (current == JsonToken.START_ARRAY)
					{
						while (jp.nextToken() != JsonToken.END_ARRAY)
						{
							JsonNode node = jp.readValueAsTree();
							IndexRequest req = Requests.indexRequest(tempindex).type(searchtype);
							JsonNode source = node.get("_source");
							if (source == null)
							{
								source = node;
							}
							String json = source.toString();
							req.source(json);

							JsonNode idNode = node.get("_id");
							if (idNode == null)
							{
								idNode = node.get("id");
							}
							if (idNode == null)
							{
								log.info("No ID found " + searchtype + " node:" + node);
							}
							else
							{
								req.id(idNode.asText());
							}
							processor.add(req);
							count++;
						}
					}
					else
					{
						log.info("Error: records should be an array: skipping.");
						jp.skipChildren();
					}
				}
				else
				{
					log.info("Unprocessed property: " + fieldName);
					jp.skipChildren();
				}

				if (count > 0 && count % 10000 == 0)
				{
					log.info("Importing: " + count + " records for " + searchtype);
				}
			}
		}
		finally
		{
			manager.flushBulk();
		}
		log.info("Imported: " + searchtype + " " + count + " records");
	}

	public void putMapping(MediaArchive mediaarchive, String searchtype, Page upload, String tempindex) throws Exception
	{
		ElasticNodeManager manager = (ElasticNodeManager) mediaarchive.getNodeManager();
		AdminClient admin = manager.getClient().admin();
		PutMappingRequest req = Requests.putMappingRequest(tempindex).updateAllTypes(true).type(searchtype);
		req = req.source(upload.getContent());
		req.validate();
		admin.indices().putMapping(req).actionGet();
	}

	// EXPORTIRNG
	public void export(ScriptLogger scriptLogger)
	{
		Searcher snapshotsearcher = getSearcherManager().getSearcher("system", "sitesnapshot");
		@SuppressWarnings("unchecked")
		HitTracker<Data> exports = snapshotsearcher.query().match("snapshotstatus", "pendingexport").search();
		if (exports.isEmpty())
		{
			throw new OpenEditException("No pending snapshotstatus  = pendingexport");
		}
		// Link files in the FileManager. Keep exports in data/system
		@SuppressWarnings("rawtypes")
		Iterator iterator = exports.iterator();
		while (iterator.hasNext())
		{
			Data snapshot = (Data) iterator.next();
			snapshot.setValue("snapshotstatus", "exporting"); // Like a lock
			snapshotsearcher.saveData(snapshot);
			Searcher sitesearcher = getSearcherManager().getSearcher("system", "site");
			Data site = sitesearcher.query().match("id", snapshot.get("site")).searchOne();
			String catalogid = site.get("catalogid");

			snapshotsearcher.saveData(snapshot);

			export(scriptLogger, catalogid, snapshot);

			snapshot.setValue("snapshotstatus", "complete");
			snapshotsearcher.saveData(snapshot);
		}
	}

	public void export(ScriptLogger scriptLogger, String inCatalogId, Data inSnap)
	{
		MediaArchive mediaarchive = (MediaArchive) getModuleManager().getBean(inCatalogId, "mediaArchive");
		PropertyDetailsArchive archive = mediaarchive.getPropertyDetailsArchive();
		List<String> searchtypes = archive.listSearchTypes();
		searchtypes.remove("modulesearch");
		searchtypes.remove("modulesearchkeyword");

		String rootfolder = "/WEB-INF/data/exports/" + mediaarchive.getCatalogId() + "/" + inSnap.get("folder");
		String catalogid = mediaarchive.getCatalogId();

		scriptLogger.info("Exporting " + rootfolder);

		boolean configonly = Boolean.parseBoolean(inSnap.get("configonly"));
		exportDatabase(scriptLogger, mediaarchive, searchtypes, rootfolder, configonly);
		Page fields = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/fields/");
		if (fields.exists())
		{
			Page target = mediaarchive.getPageManager().getPage(rootfolder + "/fields/");
			mediaarchive.getPageManager().copyPage(fields, target);
		}

		Page lists = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/lists/");
		if (lists.exists())
		{
			Page target = mediaarchive.getPageManager().getPage(rootfolder + "/lists/");
			mediaarchive.getPageManager().copyPage(lists, target);
		}

		Page views = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/views/");
		if (views.exists())
		{
			Page target = mediaarchive.getPageManager().getPage(rootfolder + "/views/");
			mediaarchive.getPageManager().copyPage(views, target);
		}

		// Collection apps = mediaarchive.getList("app");
		// for(Data app in apps)
		// {
		// String deploypath = app.get("deploypath");
		// if(deploypath != null)
		// {
		// Page page = mediaarchive.getPageManager().getPage(deploypath);
		// if (page.exists()){
		// Page target = mediaarchive.getPageManager().getPage(rootfolder + "/application/" + deploypath);
		// mediaarchive.getPageManager().copyPage(page, target);
		// }
		// }
		// }

		scriptLogger.info("Finished Exporting");

	}

	@SuppressWarnings("rawtypes")
	public void exportDatabase(ScriptLogger scriptLogger, MediaArchive mediaarchive, List<String> searchtypes, String rootfolder, boolean configonly)
	{
		String catalogid = mediaarchive.getCatalogId();
		ElasticNodeManager nodeManager = (ElasticNodeManager) mediaarchive.getNodeManager();

		String cat = mediaarchive.getCatalogId().replace("/", "_");
		String indexid = nodeManager.getIndexNameFromAliasName(cat);

		ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> indexToMappings = null;
		if (indexid != null)
		{
			scriptLogger.info("Creating Index");
			GetMappingsResponse getMappingsResponse = nodeManager.getClient().admin().indices().getMappings(new GetMappingsRequest().indices(indexid)).actionGet();
			indexToMappings = getMappingsResponse.getMappings();
			scriptLogger.info("Complete creating Index");
		}

		SearcherManager searcherManager = mediaarchive.getSearcherManager();
		scriptLogger.info("Exporting " + searchtypes.size() + " tables");
		for (String searchtype : searchtypes)
		{
			Searcher searcher = searcherManager.getSearcher(catalogid, searchtype);
			if (configonly)
			{
				if (!(searcher instanceof ElasticListSearcher))
				{
					continue;
				}
			}
			if (searcher instanceof NonExportable)
			{
				continue;
			}

			HitTracker hits = searcher.getAllHits();
			hits.enableBulkOperations();
			if (hits.size() <= 0)
			{
				continue;
			}
			if (hits.size() > 200)
			{
				scriptLogger.info("Large Table Export " + hits.size() + " records for " + searchtype);
			}

			Page output = mediaarchive.getPageManager().getPage(rootfolder + "/json/" + searchtype + ".zip");
			OutputStream os = output.getContentItem().getOutputStream();
			ZipOutputStream finalZip = new ZipOutputStream(os);
			try
			{
				ZipEntry ze = new ZipEntry(searchtype + ".json");
				finalZip.putNextEntry(ze);
				IOUtils.write("{ \"" + searchtype + "\": [", finalZip, "UTF-8");
				int size = hits.size();
				int count = 0;
				for (Iterator iterator = hits.iterator(); iterator.hasNext();)
				{
					count++;
					MultiValued hit = (MultiValued) iterator.next();
					IOUtils.write(hit.toJsonString(), finalZip, "UTF-8");
					if (size != count)
					{
						IOUtils.write(",", finalZip, "UTF-8");
					}
				}
				IOUtils.write("]}", finalZip, "UTF-8");
				finalZip.flush();
				finalZip.closeEntry();
			}
			catch (Exception ex)
			{
				throw new OpenEditException("Could not export data for " + searchtype, ex);
			}
			finally
			{
				FileUtils.safeClose(finalZip);
				FileUtils.safeClose(os);
			}

			if (indexToMappings != null)
			{
				ImmutableOpenMap<String, MappingMetaData> typeMappings = indexToMappings.get(indexid);
				if (typeMappings != null)
				{
					MappingMetaData actualMapping = typeMappings.get(searchtype);
					if (actualMapping != null)
					{
						try
						{
							String json = actualMapping.source().string();
							Page mappings = mediaarchive.getPageManager().getPage(rootfolder + "/json/" + searchtype + "-mapping.json");
							mediaarchive.getPageManager().saveContent(mappings, null, json, "Saved mapping");
						}
						catch (Exception ex)
						{
							scriptLogger.error("Could not save mapping for " + searchtype, ex);
						}
					}
					else
					{
						scriptLogger.info("No mapping found for " + searchtype);
					}
				}
			}
		}
	}

}
