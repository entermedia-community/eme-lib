package org.entermediadb.ai.skills;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.hittracker.HitTracker;

public class GoalProcessorSkill extends BaseSkill
{

	// Monitor open Goals and send them to the Goal Planner
	@Override
	public void process(AgentContext inContext)
	{

		// Search the DB for any open goals and send them to the Goal Planner
		HitTracker all = getMediaArchive().query("projectgoal").exact("projectstatus", "open").sort("creationdate").search();

		// Looo over all tasks that are not

		// requested
		Collection<String> goalids = all.collectValues("id");
		if (goalids.isEmpty())
		{
			// No goals pending
			inContext.log("no goals pending");
			return;
		}
		HitTracker tasks = getMediaArchive().query("goaltask").orgroup("projectgoal", goalids).exact("taskstatus", "0").sort("creationdate").search();

		if (tasks.isEmpty())
		{
			inContext.log("no tasks pending");
			return;
		}

		// foreach task see if there are any roles on it that need help
		for (Object taskObj : tasks)
		{
			MultiValued task = (MultiValued) taskObj;

			/*
			 * Collection<Map> roles = (Collection<Map>) task.getValue("taskroles"); if (roles == null ||
			 * roles.isEmpty()) {
			 * 
			 * continue; }
			 */
			String goalid = task.get("projectgoal");
			MultiValued goal = (MultiValued) getMediaArchive().getData("projectgoal", goalid);
			if (goal == null)
			{
				inContext.log("Goal not found for task: " + task.getId());
				continue;
			}

			inContext.log("Processing task: " + task.getId() + " for goal: " + goal.getId());

			Data channel = getMediaArchive().query("channel").exact("dataid", task.getId()).searchOne();
			if (channel == null)
			{
				// CREATE
				channel = getMediaArchive().getSearcher("channel").createNewData();
				channel.setValue("dataid", task.getId());
				channel.setValue("searchtype", "goaltask");
				channel.setValue("user", goal.get("owner"));
				channel.setValue("date", new Date());
				channel.setValue("channeltype", "agententitychat");
				getMediaArchive().saveData("channel", channel);
			}

			HitTracker<MultiValued> messages = getMediaArchive().query("chatterbox").exact("channel", channel.getId()).sort("date").search();

			String taskname = task.getName();
			if (taskname == null)
			{
				taskname = task.get("comment");
			}

			if (messages.isEmpty())
			{
				// Make a mesage that sets the plan for this task. So that AI will respond to it.
				Data newMessage = getMediaArchive().getSearcher("chatterbox").createNewData();
				newMessage.setValue("channel", channel.getId());
				newMessage.setValue("taskstatus", "pendinguserapproval");
				/*
				 * String collectionid = goal.get("collectionid"); Collection<String> docids =
				 * loadOwnerKnowlege(collectionid, goal.get("owner"));
				 * 
				 * // Loop over roleuserids
				 * 
				 * for (Map role : roles) { String userid = (String) role.get("roleuserid"); Collection<String>
				 * roledocs = loadOwnerKnowlege(userid); docids.addAll(roledocs); }
				 * 
				 * inContext.putContextValue("docids", docids);
				 * 
				 * // Call AI to create a message LlmConnection llmconnection =
				 * getMediaArchive().getLlmConnection("embedding"); Map payload = new HashMap();
				 * 
				 * 
				 * String requestedtask = taskname + " within the goal of " + goal.getName();
				 * 
				 * String prompt = "Create some steps needed for an AI agent to offer help for the task: " +
				 * requestedtask;
				 * 
				 * payload.put("query", prompt);
				 * 
				 * payload.put("parent_ids", docids); // log.info("Sending: " + payload); LlmResponse response =
				 * llmconnection.callJson("/create_outline", payload);
				 * 
				 * JSONObject outlineJson = response.getRawResponse(); Collection<String> outline =
				 * (Collection<String>) outlineJson.get("outline");
				 * 
				 * String message = "The following steps are suggested for the task: " + taskname + "\n"; for
				 * (String step : outline) { message += "- " + step + "\n"; }
				 */
				String message = taskname;
				newMessage.setValue("message", message);
				newMessage.setValue("chatmessagestatus", "received");
				newMessage.setValue("channel", channel.getId());
				newMessage.setValue("date", new Date());
				newMessage.setValue("messagetype", "message");
				newMessage.setValue("user", goal.get("owner"));
				newMessage.setValue("useralias", goal.get("owner"));
				newMessage.setValue("functionname", "emeteamchat_responder_respond");
				newMessage.setValue("agentcontextvalues", "{\"currentscenario\":\"emeteamchat_responder\"}");
				getMediaArchive().saveData("chatterbox", newMessage);

				channel.setValue("refreshdate", new Date());
				getMediaArchive().saveData("channel", channel);
				// TODO: Kick off the AI monitor event
				getMediaArchive().fireSharedMediaEvent("llm.monitorchats");
			}
		}
		super.process(inContext);
	}

}
