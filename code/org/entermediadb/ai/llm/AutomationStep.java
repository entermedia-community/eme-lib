package org.entermediadb.ai.llm;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.Skill;
import org.json.simple.JSONObject;
import org.openedit.MultiValued;

public class AutomationStep
{
	private static final Log log = LogFactory.getLog(AutomationStep.class);

	AutomationStep fieldParentAutomationStep;

	public AutomationStep getParentAutomationStep()
	{
		return fieldParentAutomationStep;
	}

	public void setParentAutomationStep(AutomationStep inParentAutomationStep)
	{
		fieldParentAutomationStep = inParentAutomationStep;
	}

	MultiValued fieldAgentData;

	public MultiValued getAgentData()
	{
		return fieldAgentData;
	}

	public Object getValue(String inField)
	{
		Object value = getAutomationStepData().getValue(inField);
		if (value == null)
		{
			value = getAgentData().getValue(inField);
		}
		return value;
	}

	public String get(String inField)
	{
		String value = getAutomationStepData().get(inField);
		if (value == null)
		{
			value = getAgentData().get(inField);
		}
		return value;
	}

	public void setAgentData(MultiValued inAgentData)
	{
		fieldAgentData = inAgentData;
	}

	MultiValued fieldAutomationStepData;

	public MultiValued getAutomationStepData()
	{
		return fieldAutomationStepData;
	}

	public void setAutomationStepData(MultiValued inAutomationStepData)
	{
		fieldAutomationStepData = inAutomationStepData;
	}

	public Skill getAgent()
	{
		return fieldAgent;
	}

	public void setAgent(Skill inAgent)
	{
		fieldAgent = inAgent;
	}

	Skill fieldAgent;

	public String getParentAgent()
	{
		String runafter = getAutomationStepData().get("runafter");
		return runafter;
	}

	Collection<AutomationStep> fieldChildren;

	public Collection<AutomationStep> getChildren()
	{
		if (fieldChildren == null)
		{
			fieldChildren = new ArrayList();
		}
		return fieldChildren;
	}

	public void setChildren(Collection<AutomationStep> inChildren)
	{
		fieldChildren = inChildren;
	}

	public AutomationStep getChildren(String inId)
	{
		AutomationStep selected = null;
		for (AutomationStep child : getChildren())
		{
			String id = child.get("id");
			if (id.equals(inId))
			{
				selected = child;
				break;
			}
		}
		return selected;
	}

	public void addChild(AutomationStep inChildAgent)
	{
		getChildren().add(inChildAgent);
		inChildAgent.setParentAutomationStep(this);
	}

	@Override
	public String toString()
	{
		return String.valueOf(getAgentData());
	}

	Collection<JSONObject> fieldAgentParameterStructure;

	public Collection<JSONObject> getAgentParameterStructure()
	{
		return fieldAgentParameterStructure;
	}

	public void setAgentParameterStructure(Collection<JSONObject> inAgentParameterStructure)
	{
		fieldAgentParameterStructure = inAgentParameterStructure;
	}

	JSONObject fieldAgentParameterValues;

	public JSONObject getAgentParameterValues()
	{
		return fieldAgentParameterValues;
	}

	public void setAgentParameterValues(JSONObject inAgentParameterValues)
	{
		fieldAgentParameterValues = inAgentParameterValues;
	}

	protected JSONObject fieldExtraContextValues;

	public JSONObject getExtraContextValues()
	{
		return fieldExtraContextValues;
	}

	public void setExtraContextValues(JSONObject inExtraContextValues)
	{
		fieldExtraContextValues = inExtraContextValues;
	}

	public String getEnabledId()
	{
		return getAutomationStepData().getId();
	}

	public AutomationStep getNextAutomationStep()
	{
		if (getChildren() != null && getChildren().size() > 0)
		{
			return getChildren().iterator().next();
		}
		return null;
	}
}
