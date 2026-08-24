package org.entermediadb.ai.skills;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.TutorMessageContext;
import org.entermediadb.ai.automation.RunningScenario;
import org.entermediadb.ai.llm.AgentEnabled;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;

public class AdaptiveTutorialBaseSkill extends BaseSkill
{
	public void endTutorial(TutorMessageContext tutorMessageContext)
	{
		RunningScenario scenario = tutorMessageContext.getCurrentScenario();

		AgentEnabled nextAgentEnabled = scenario.findEnabled("chat_tutor_end");

		TutorMessageContext nextContext = (TutorMessageContext) scenario.createAgentContext(tutorMessageContext, nextAgentEnabled);
		scenario.runProcess(nextAgentEnabled, nextContext, true);
	}

	public Map<String, Double> getCognitiveLevelPoints()
	{
		Collection<MultiValued> mcqcognitivelevels = getMediaArchive().query("mcqcognitivelevel").all().search();
		Map<String, Double> cognitivelevelpoints = new HashMap<>();
		for (MultiValued level : mcqcognitivelevels)
		{
			cognitivelevelpoints.put(level.getId(), level.getDouble("points"));
		}
		return cognitivelevelpoints;
	}

	public Map<String, Double> getAnswerConfidenceBonus()
	{
		Collection<MultiValued> answerconfidences = getMediaArchive().query("answerconfidence").all().search();
		Map<String, Double> answerconfidencebonus = new HashMap<>();
		for (MultiValued confidence : answerconfidences)
		{
			answerconfidencebonus.put(confidence.getId(), confidence.getDouble("bonuspercentage"));
		}
		return answerconfidencebonus;
	}

	protected JSONArray getReleaventChatHistory(String channelId, String userId)
	{
		Collection<MultiValued> messages = getMediaArchive().query("chatterbox").exact("channel", channelId).orgroup("user", userId + ",agent").not("messagetype", "system").sort("dateDown").search();

		List<MultiValued> releaventMessages = new ArrayList<>();

		for (MultiValued message : releaventMessages)
		{
			JSONObject agentContext = message.getJSONValue("agentcontextvalues");
			if (agentContext == null || agentContext.get("componentid") == null)
			{
				continue;
			}
			releaventMessages.add(message);
			if ("question".equals(agentContext.get("messagetype")))
			{
				break;
			}
		}

		List<MultiValued> reversedMessages = new ArrayList<>(messages);
		Collections.reverse(reversedMessages);

		JSONArray chatHistory = new JSONArray();
		for (MultiValued msg : reversedMessages)
		{
			JSONObject item = buildHistoryFromAgentContext(msg.getJSONValue("agentcontextvalues"));
			if (item != null)
			{
				chatHistory.add(item);
			}
		}

		return chatHistory;
	}

	protected JSONObject buildHistoryFromAgentContext(JSONObject agentContext)
	{
		JSONObject historyItem = new JSONObject();
		historyItem.put("role", "assistant");

		StringBuilder ctx = new StringBuilder();

		if ("question".equals(agentContext.get("messagetype")))
		{
			JSONObject question = (JSONObject) agentContext.get("question");
			if (question != null)
			{
				ctx.append(question.get("question")).append("\n");
				JSONObject options = (JSONObject) question.get("options");
				if (options != null)
				{
					ctx.append(options.toJSONString()).append("\n");
					if (options.get("option_a") != null)
					{
						ctx.append("option_a: " + options.get("option_a")).append("\n");
					}
					if (options.get("option_b") != null)
					{
						ctx.append("option_b: " + options.get("option_b")).append("\n");
					}
					if (options.get("option_c") != null)
					{
						ctx.append("option_c: " + options.get("option_c")).append("\n");
					}
					if (options.get("option_d") != null)
					{
						ctx.append("option_d: " + options.get("option_d")).append("\n");
					}
					if (options.get("option_e") != null)
					{
						ctx.append("option_e: " + options.get("option_e")).append("\n");
					}
					if (options.get("option_f") != null)
					{
						ctx.append("option_f: " + options.get("option_f")).append("\n");
					}
				}

				if (question.get("correctoption") != null)
				{
					ctx.append("\n").append("Correct Option: " + question.get("correctoption")).append("\n");
				}
				if (question.get("rationale") != null)
				{
					ctx.append("\n").append("Rationale: " + question.get("rationale")).append("\n");
				}
			}
		}
		else if ("answereval".equals(agentContext.get("messagetype")))
		{
			historyItem.put("role", "user");
			ctx.append("\n").append("My answer: " + agentContext.get("selectedoption")).append("\n");
			ctx.append("\n").append("Confidence: " + agentContext.get("confidence")).append("\n");
		}
		else if ("text".equals(agentContext.get("messagetype")))
		{
			ctx.append("\n").append(agentContext.get("componentcontent")).append("\n");
		}
		else if ("asset".equals(agentContext.get("messagetype")))
		{
			// TODO
		}
		else if ("usercomment".equals(agentContext.get("messagetype")))
		{
			historyItem.put("role", "user");
		}

		String content = ctx.toString();

		if (content.length() == 0)
		{

			return null;
		}

		historyItem.put("content", content);

		return historyItem;
	}

}
