package org.entermediadb.ai.skills;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.TutorMessageContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.openedit.MultiValued;

public class AdaptiveTutorialBaseSkill extends BaseSkill
{
	public void endTutorial(TutorMessageContext messageContext)
	{
		AgentEnabled currentAgentEnabled = messageContext.getCurrentScenario().findEnabled("chat_tutor_end");

		messageContext.setCurrentAgentEnable(currentAgentEnabled);
		messageContext.fireStatusComplete(currentAgentEnabled);
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
}
