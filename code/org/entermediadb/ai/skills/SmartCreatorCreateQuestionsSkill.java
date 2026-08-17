package org.entermediadb.ai.skills;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.Searcher;

public class SmartCreatorCreateQuestionsSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(SmartCreatorCreateQuestionsSkill.class);

	private static final String[] QUESTION_CHOICES = {"option_a", "option_b", "option_c", "option_d"};

	public ChatSmartCreatorConfirmationSkill getSmartCreatorSkill()
	{
		ChatSmartCreatorConfirmationSkill smartCreatorManager = (ChatSmartCreatorConfirmationSkill) getMediaArchive().getBean("smartCreatorSkill");
		return smartCreatorManager;
	}

	@Override
	public void process(AgentContext inContext)
	{
		makeQuestionsFromContents(inContext);
		super.process(inContext);
	}

	public void makeQuestionsFromContents(AgentContext inContext)
	{
		String playbackentitymoduleid = (String) inContext.getContextValue("playbackentitymoduleid");
		String playbackentityid = (String) inContext.getContextValue("playbackentityid");

		Searcher sectionSearcher = getMediaArchive().getSearcher("componentsection");
		Searcher componentSearcher = getMediaArchive().getSearcher("componentcontent");
		Searcher questionSearcher = getMediaArchive().getSearcher("entityquestion");

		Collection<Data> componentsSection = sectionSearcher.query().exact("playbackentitymoduleid", playbackentitymoduleid).exact("playbackentityid", playbackentityid).search();

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("thinking");

		for (Data section : componentsSection)
		{
			String sectionid = section.getId();
			Collection<MultiValued> componentcontents = componentSearcher.query().exact("componentsectionid", sectionid).sort("ordering").search();

			String contextcontent = "";

			int ordering = 0;

			for (MultiValued componentcontent : componentcontents)
			{
				Integer currentOrdering = componentcontent.getInt("ordering");

				if (currentOrdering != null)
				{
					ordering = Math.max(ordering, currentOrdering);
				}

				String componenttype = componentcontent.get("componenttype");
				if ("asset".equals(componenttype))
				{
					continue;
				}
				if ("mcq".equals(componenttype))
				{
					continue;
				}
				contextcontent += componentcontent.get("content");
			}

			if (contextcontent.trim().length() == 0)
			{
				log.info("Skipping section because it has no content: " + sectionid);
				// TODO: Use embedding server to use just the section title to generate questions
				continue;
			}

			inContext.putContextValue("contextcontent", contextcontent);
			LlmResponse response = llmconnection.callStructure(inContext, "smartcreator_questions");
			JSONObject rawResponse = response.getMessageStructured();
			Collection<Map> questions = (Collection<Map>) rawResponse.get("questions");

			for (Map questionmap : questions)
			{
				String questiontext = (String) questionmap.get("question");
				List<String> choices = (List<String>) questionmap.get("choices");
				Object correct_answer_index = questionmap.get("correct_answer_index");
				String rationale = (String) questionmap.get("rationale");
				Integer correctindex = null;
				if (correct_answer_index instanceof Long)
				{
					correctindex = ((Long) correct_answer_index).intValue();
				}
				else if (correct_answer_index instanceof String)
				{
					correctindex = Integer.parseInt((String) correct_answer_index);
				}
				if (correctindex == null)
				{
					continue;
				}

				if (questiontext == null || questiontext.trim().length() == 0 || choices == null || choices.size() < 4 || correctindex < 0 || correctindex >= choices.size())
				{
					log.info("Skipping question because it is missing text or choices: " + questionmap);
					continue;
				}

				Object cognitive_level = questionmap.get("cognitive_level");
				if (cognitive_level == null)
				{
					cognitive_level = "beginner";
				}

				Data question = questionSearcher.createNewData();
				question.setValue("question", questiontext);
				for (int i = 0; i < QUESTION_CHOICES.length; i++)
				{
					question.setValue(QUESTION_CHOICES[i], choices.get(i));
				}
				question.setValue("correctoption", QUESTION_CHOICES[correctindex]);
				question.setValue("mcqcognitivelevel", cognitive_level);
				question.setValue("rationale", rationale);
				questionSearcher.saveData(question);

				Data componentContent = componentSearcher.createNewData();
				componentContent.setValue("componenttype", "mcq");
				componentContent.setValue("questionid", question.getId());
				componentContent.setValue("modificationdate", new Date());
				componentContent.setValue("componentsectionid", sectionid);
				ordering += 1;
				componentContent.setValue("ordering", ordering);
				componentSearcher.saveData(componentContent);
			}
		}
	}
}
