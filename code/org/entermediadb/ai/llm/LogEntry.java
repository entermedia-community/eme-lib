package org.entermediadb.ai.llm;

import java.util.Date;

import org.openedit.Data;

public class LogEntry
{
	public LogEntry(String inType, String inMessage) {
		setLogType(inType);
		setMessage(inMessage);
	}

	protected Date fieldDate;

	public Date getDate()
	{
		return fieldDate;
	}

	public void setDate(Date inDate)
	{
		fieldDate = inDate;
	}

	protected String fieldLogType;

	public String getLogType()
	{
		return fieldLogType;
	}

	public void setLogType(String inLogType)
	{
		fieldLogType = inLogType;
	}

	public String getMessage()
	{
		return fieldMessage;
	}

	public void setMessage(String inMessage)
	{
		fieldMessage = inMessage;
	}

	public Data getCurrentAutomationStepData()
	{
		return fieldCurrentAutomationStepData;
	}

	public void setCurrentAutomationStepData(Data inCurrentAutomationStepData)
	{
		fieldCurrentAutomationStepData = inCurrentAutomationStepData;
	}

	protected String fieldMessage;
	protected Data fieldCurrentAutomationStepData;
	protected Data fieldCurrentAgentData;

	public Object getValue(String inField)
	{
		Object value = getCurrentAutomationStepData().getValue(inField);
		if (value == null)
		{
			value = getAgentData().getValue(inField);
		}
		return value;
	}

	private Data getAgentData()
	{
		return fieldCurrentAgentData;
	}

	public void setAgentData(Data inAgentData)
	{
		fieldCurrentAgentData = inAgentData;
	}

	public String get(String inField)
	{
		String value = getCurrentAutomationStepData().get(inField);
		if (value == null)
		{
			value = getAgentData().get(inField);
		}
		return value;
	}

}
