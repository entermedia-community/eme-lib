package org.entermediadb.ai.skills;

import java.util.HashMap;
import java.util.Map;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.TutorMessageContext;
import org.entermediadb.ai.automation.RunningScenario;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.MultiValued;

public class AdaptiveTutorialWelcomeSkill extends AdaptiveTutorialBaseSkill
{
	@Override
	public void process(AgentContext inAgentContext)
	{
		TutorMessageContext messageContext = (TutorMessageContext) inAgentContext;

		messageContext.putContextValue("skiploader", Boolean.TRUE);

		String tutorialid = (String) messageContext.getContextValue("tutorialid");
		messageContext.setTutorialId(tutorialid);

		MultiValued tutorial = (MultiValued) getMediaArchive().query("entitytutorial").exact("id", tutorialid).searchOne();

		messageContext.putContextValue("tutorial", tutorial);

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("localrender");
		LlmResponse response = llmconnection.renderLocalAction(inAgentContext, "chat_tutor_welcome");
		messageContext.setLastResponse(response);
		messageContext.log("sent" + response.getMessagePlain());

		messageContext.setLastSectionId(null);
		messageContext.setLastComponentId(null);

		MultiValued agentmessage = messageContext.getAgentMessage();
		agentmessage.setValue("chatmessagestatus", "completed");
		agentmessage.setValue("messagetype", "message");
		Map<String, String> broadcastpayload = new HashMap<String, String>();
		broadcastpayload.put("messageid", tutorialid + "_welcome");
		messageContext.setValue("broadcastpayload", broadcastpayload);

		AgentEnabled skillEnabled = messageContext.getCurrentAgentEnable();
		messageContext.fireStatusComplete(skillEnabled);

		RunningScenario scenario = messageContext.getCurrentScenario();

		AgentEnabled nextAgentEnabled = scenario.findEnabled("chat_tutor_continue");
		TutorMessageContext nextContext = (TutorMessageContext) scenario.createAgentContext(messageContext, nextAgentEnabled);
		nextContext.setWaitTime(200l);
		scenario.runProcess(nextAgentEnabled, nextContext, true);
	}

}
