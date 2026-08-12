package org.entermediadb.ai.skills;

import java.util.Collection;
import java.util.Iterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.ChatMessageContext;
import org.entermediadb.ai.Schema;
import org.entermediadb.ai.assistant.AssistantManager;
import org.entermediadb.ai.assistant.GuideStatus;
import org.entermediadb.ai.classify.EmbeddingManager;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.Asset;
import org.json.simple.JSONArray;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.hittracker.HitTracker;

public class QuestionsAskSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(QuestionsAskSkill.class);

	@Override
	public void process(AgentContext inAgentContext)
	{
		ChatMessageContext messageContext = (ChatMessageContext) inAgentContext;
		///
		MultiValued inAgentMessage = messageContext.getAgentMessage();

		MultiValued usermessage = (MultiValued) getMediaArchive().getCachedData("chatterbox", inAgentMessage.get("replytoid"));
		String query = usermessage.get("message");

		// Make sure they have already picked the documents
		String entiyid = messageContext.get("entityid");
		String moduleid = messageContext.get("entitymoduleid");

		if (entiyid != null && moduleid != null)
		{
			Data entity = getMediaArchive().getData(moduleid, entiyid);
			if (entity != null)
			{
				AssistantManager assistant = (AssistantManager) getMediaArchive().getBean("assistantManager");
				Collection<String> docids = (Collection<String>) messageContext.getContextValue("docids");
				if (docids == null || docids.isEmpty())
				{
					docids = assistant.findDocIdsForEntity(moduleid, entiyid);
				}
				
				EmbeddingManager embeddings = (EmbeddingManager) getMediaArchive().getBean("embeddingManager");
				LlmResponse response = embeddings.findAnswer(messageContext, docids, query);
				messageContext.setLastResponse(response);
				messageContext.setWaitTime(null);
				AgentEnabled skillEnabled = messageContext.getCurrentAgentEnable();
				messageContext.fireStatusComplete(skillEnabled);
				return;
			}
		}
		LlmResponse response = searchSystemWide(messageContext, query);
		messageContext.setLastResponse(response);

		return;

	}

	protected LlmResponse searchSystemWide(AgentContext messageContext, String query)
	{
		Schema schema = loadSchema();

		Collection<String> moduleids = schema.getModuleIds();

		HitTracker hits = getMediaArchive().query("modulesearch").put("searchtypes", moduleids).freeform("description", query).exact("entityembeddingstatus", "embedded").search();

		Collection<String> docids = new JSONArray();
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			MultiValued doc = (MultiValued) iterator.next();
			String searchtype = doc.get("entitysourcetype");
			String docid = searchtype + "_" + doc.getId();
			docids.add(docid);
		}

		EmbeddingManager embeddings = (EmbeddingManager) getMediaArchive().getBean("embeddingManager");
		LlmResponse response = embeddings.findAnswer(messageContext, docids, query);
		return response;
	}

	protected String getMarkdown(Data data)
	{
		String markdown = data.get("markdowncontent");
		if (markdown == null)
		{
			markdown = data.get("maincontent");
			if (markdown == null)
			{
				markdown = data.get("longcaption");
			}
		}
		if (markdown == null)
		{
			String assetid = data.get("primarymedia");
			Asset asset = getMediaArchive().getEntityManager().getAsset(data);
			if (asset != null)
			{
				markdown = asset.get("longcaption");
			}
		}

		if (markdown == null || markdown.isEmpty())
		{
			markdown = data.getName();
		}
		return markdown;
	}

	/*
	 * protected void handleLlmResponse(AgentContext messageContext, LlmResponse response) { //TODO: Use
	 * IF statements to sort what parsing we need to do. parseSearchParams parseWorkflowParams etc
	 * JSONObject content = response.getMessageStructured();
	 * 
	 * String toolname = (String) content.get("next_step"); //request_type
	 * 
	 * if(toolname == null) { throw new OpenEditException("No type specified in results: " +
	 * content.toJSONString()); }
	 * 
	 * JSONObject details = (JSONObject) content.get("step_details");
	 * 
	 * if(details == null) { throw new OpenEditException("No details specified in results: " +
	 * content.toJSONString()); } if( toolname.equals("conversation")) { JSONObject conversation =
	 * (JSONObject) details.get("conversation"); String generalresponse = (String)
	 * conversation.get("friendly_response");
	 * 
	 * response.setMessage( generalresponse); // response.setFunctionName("conversation"); } else if(
	 * toolname.equals("question_search") ) { JSONObject structure = (JSONObject) details.get(toolname);
	 * if(structure == null) { throw new OpenEditException("No structure found for type: " + toolname);
	 * } String search_keyword = (String) structure.get("search_keyword");
	 * messageContext.setValue("search_keyword", search_keyword); //Search system wise for keyword hits
	 * that are embedded }
	 * 
	 * response.setFunctionName(toolname); }
	 */

	public String getAnswerByEntity(String inModuleId, String inEntityid, String inQuestion)
	{
		AssistantManager assistant = (AssistantManager) getMediaArchive().getBean("assistantManager");
		Collection<String> docIds = assistant.findDocIdsForEntity(inModuleId, inEntityid);
		EmbeddingManager embeddings = (EmbeddingManager) getMediaArchive().getBean("embeddingManager");
		String answer = embeddings.findAnswer(docIds, inQuestion);
		return answer;
	}

}
