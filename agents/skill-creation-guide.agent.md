---
agent_id: skillCreator
agent_name: Skill Agent Creator
agent_type: documentation_agent
agent_version: 1.0.0
description: Guide for creating new skill-based agents with complete integration into the EME system
---

# Skill Agent Creation Guide

This guide provides comprehensive instructions for creating new skill agents in the EME system, including all necessary files, configurations, and integration points.

## Overview

A skill agent in EME consists of:
1. **Java Skill Class** - The implementation logic
2. **aiskill XML Entry** - Catalog registration with skilloverview
3. **plugin.xml Bean Definition** - Spring framework integration
4. **Optional: Agent Configuration File** - Detailed agent metadata (.agent.md)

## Step 1: Define Your Skill

Before creating any files, define:
- **Skill Name**: Display name (e.g., "Web Content Extractor")
- **Skill ID**: camelCase bean identifier (e.g., "webContentExtractorSkill")
- **Skill Class Name**: Java class name (e.g., "WebContentExtractorSkill")
- **Description**: Brief explanation of what the skill does
- **Skill Overview**: Short summary for catalog display (1-2 sentences)
- **Context Fields**: Fields the skill will use from AgentContext
- **Operations**: What the skill can do (read, write, process, etc.)

### Naming Conventions

| Component | Format | Example |
|-----------|--------|---------|
| Skill Name | Title Case | "Web Content Extractor" |
| Skill ID | camelCase | "webContentExtractorSkill" |
| Class Name | PascalCase + Skill | "WebContentExtractorSkill" |
| XML Filename | snake_case | "web_content_extractor.xml" |
| Package | lowercase.dots | "org.entermediadb.ai.skills" |

## Step 2: Create Java Skill Class

### File Path
```
/eme-server/plugins/finder/code/org/entermediadb/ai/skills/{ClassName}.java
```

### Class Structure Template

```java
package org.entermediadb.ai.skills;

import java.util.Date;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;

/**
 * {ClassName} - {Description}
 * 
 * This skill is designed to:
 * - Action 1
 * - Action 2
 * - Action 3
 * 
 * Usage in AgentContext:
 * - Set "{contextfield1}" for...
 * - Set "{contextfield2}" for...
 * 
 * Results stored in context:
 * - "{resultfield1}" - Description
 * - "{resultfield2}" - Description
 */
public class {ClassName} extends BaseSkill
{
	private static final Log log = LogFactory.getLog({ClassName}.class);

	@Override
	public void process(AgentContext inContext)
	{
		try
		{
			// Extract context values
			Object input = inContext.getContextValue("inputfield");
			
			// Implement skill logic
			log.info("{ClassName}: Processing started");
			
			// Set results
			inContext.putContextValue("operationstatus", "success");
			inContext.putContextValue("operationdate", new Date());
		}
		catch (Exception e)
		{
			log.error("{ClassName}: Error during processing", e);
			inContext.putContextValue("operationstatus", "error");
			inContext.putContextValue("errormessage", e.getMessage());
		}

		super.process(inContext);
	}
}
```

### Key Implementation Requirements

- **Extend BaseSkill**: All skills must extend `BaseSkill`
- **Implement process()**: Override the `process(AgentContext inContext)` method
- **Extract Context Values**: Use `inContext.getContextValue("fieldname")`
- **Set Results**: Use `inContext.putContextValue("fieldname", value)`
- **Error Handling**: Catch exceptions and set error status
- **Logging**: Use `log.info()` and `log.error()` for debugging
- **Set Status**: Always set "operationstatus" to "success" or "error"

## Step 3: Create aiskill XML Catalog Entry

### File Path
```
/eme-server/plugins/catalog/html/data/lists/aiskill/{skillname}.xml
```

### XML Structure Template

```xml
<?xml version="1.0" encoding="UTF-8"?>

<results>

  <data id="{skillId}" bean="{skillId}" agenttype="taskagent" ordering="10" skilloverview="{skillOverview}">
    <name>
      <language id="en"><![CDATA[{SkillName}]]></language>
      <language id="de"><![CDATA[{SkillNameDE}]]></language>
      <language id="fr"><![CDATA[{SkillNameFR}]]></language>
      <language id="es"><![CDATA[{SkillNameES}]]></language>
    </name>
  </data>

</results>
```

### XML Attributes Explained

| Attribute | Value | Example | Description |
|-----------|-------|---------|-------------|
| `id` | camelCase | `webContentExtractorSkill` | Unique identifier |
| `bean` | camelCase | `webContentExtractorSkill` | Must match Java class bean ID |
| `agenttype` | taskagent/remoteagent | `taskagent` | Type of agent |
| `ordering` | integer | `10` | Display order in UI |
| `skilloverview` | string | `Extracts text from web pages...` | Short description for catalog |

### skilloverview Best Practices

- **Length**: 1-2 sentences maximum
- **Clarity**: Describe what the skill does in simple terms
- **Action-Oriented**: Start with verb (e.g., "Extracts", "Generates", "Processes")
- **Escape XML**: Replace `&` with `&amp;`, `<` with `&lt;`, `>` with `&gt;`

**Example skilloverview values:**
- "Extracts and reads content from specified websites by making HTTP requests and parsing HTML."
- "Reads and writes text content from files with support for path resolution and overwrite protection."
- "Generates new skill class templates and configuration files based on skill descriptions."

## Step 4: Add Bean Definition to plugin.xml

### File Path
```
/eme-server/plugins/finder/html/src/plugin.xml
```

### Location in File

Find the section with other skill bean definitions (around line 3200+). Look for existing skill beans:
```xml
<bean id="embeddingFinderSkill" class="org.entermediadb.ai.skills.EmbeddingFinderSkill" ...>
<bean id="webContentExtractorSkill" class="org.entermediadb.ai.skills.WebContentExtractorSkill" ...>
```

### Bean Definition Template

```xml
<bean id="{skillId}" class="org.entermediadb.ai.skills.{ClassName}" lazy-init="default" scope="prototype">
	<property name="moduleManager">
		<ref bean="moduleManager" />
	</property>
</bean>
```

### Bean Attributes

| Attribute | Value | Purpose |
|-----------|-------|---------|
| `id` | camelCase | Bean identifier (must match skillId) |
| `class` | full.package.ClassName | Full Java class path |
| `lazy-init` | default | Spring lifecycle option |
| `scope` | prototype | Creates new instance per use |
| `property name="moduleManager"` | required | Module manager injection |

### Insertion Points

- **Before**: Insert before the closing `</beans>` tag
- **After**: Insert after other skill bean definitions
- **Line Numbers**: Typically around line 3220-3250 (check current plugin.xml)

## Step 5: Directory Structure Summary

When creating a new skill, ensure these file paths are created/updated:

```
/eme-server/
├── plugins/
│   ├── finder/
│   │   ├── code/org/entermediadb/ai/skills/
│   │   │   └── {ClassName}.java                    ← Step 2
│   │   └── html/src/
│   │       └── plugin.xml                          ← Step 4 (EDIT EXISTING)
│   └── catalog/
│       └── html/data/lists/aiskill/
│           └── {skillname}.xml                     ← Step 3
```

## Step 6: Optional - Create Agent Configuration File

For complex skills, optionally create a `.agent.md` file for detailed documentation:

### File Path
```
/eme-server/plugins/finder/agents/{skillname}.agent.md
```

### Configuration Template

```markdown
---
agent_id: {skillId}
agent_name: {SkillName}
agent_type: skill_agent
agent_version: 1.0.0
created_date: {current-date}
---

# {SkillName}

## Description
{Detailed description of what the skill does}

## Capabilities
- Capability 1
- Capability 2
- Capability 3

## Context Fields Used
- `fieldname1` - Description
- `fieldname2` - Description

## Results Provided
- `resultfield1` - Description
- `resultfield2` - Description

## Integration Points
1. Bean Definition: `plugins/finder/html/src/plugin.xml`
2. Catalog Entry: `plugins/catalog/html/data/lists/aiskill/{skillname}.xml`
3. Implementation: `plugins/finder/code/org/entermediadb/ai/skills/{ClassName}.java`

## Usage Example
```java
inContext.putContextValue("fieldname", value);
// ... call skill
String result = (String) inContext.getContextValue("resultfield");
```
```

## Complete Skill Integration Checklist

Use this checklist when creating a new skill:

- [ ] **Java Class Created**
  - [ ] File: `/plugins/finder/code/org/entermediadb/ai/skills/{ClassName}.java`
  - [ ] Extends `BaseSkill`
  - [ ] Implements `process(AgentContext)`
  - [ ] Proper error handling and logging
  - [ ] Context values extracted and results set

- [ ] **aiskill XML Entry Created**
  - [ ] File: `/plugins/catalog/html/data/lists/aiskill/{skillname}.xml`
  - [ ] Correct `id` and `bean` attributes (must match)
  - [ ] `skilloverview` attribute populated (1-2 sentences)
  - [ ] Multi-language names provided
  - [ ] XML well-formed (valid XML syntax)

- [ ] **plugin.xml Bean Definition Added**
  - [ ] File: `/plugins/finder/html/src/plugin.xml`
  - [ ] Bean ID matches skill ID
  - [ ] Class path correct
  - [ ] moduleManager property included
  - [ ] Inserted before closing `</beans>` tag

- [ ] **Testing**
  - [ ] Java class compiles without errors
  - [ ] XML files validate
  - [ ] Plugin.xml still parses correctly
  - [ ] Skill appears in catalog list
  - [ ] Bean can be injected/used

## Example: Complete Skill Creation Walkthrough

### Scenario
Create a skill called "Text Analyzer" that analyzes text content and returns statistics.

### 1. Define Parameters
- **Skill Name**: "Text Analyzer"
- **Skill ID**: "textAnalyzerSkill"
- **Class Name**: "TextAnalyzerSkill"
- **Description**: "Analyzes text content and provides statistics"
- **skillOverview**: "Analyzes text content to extract word count, character count, sentence count, and keyword frequency."

### 2. Create Java Class
File: `TextAnalyzerSkill.java`
```java
package org.entermediadb.ai.skills;

public class TextAnalyzerSkill extends BaseSkill {
    // Implementation...
    // Extract: inContext.getContextValue("textcontent")
    // Return: putContextValue("wordcount", count)
}
```

### 3. Create XML Entry
File: `text_analyzer.xml`
```xml
<data id="textAnalyzerSkill" bean="textAnalyzerSkill" agenttype="taskagent" ordering="10" 
      skilloverview="Analyzes text content to extract word count, character count, sentence count, and keyword frequency.">
```

### 4. Add Bean to plugin.xml
```xml
<bean id="textAnalyzerSkill" class="org.entermediadb.ai.skills.TextAnalyzerSkill" lazy-init="default" scope="prototype">
	<property name="moduleManager">
		<ref bean="moduleManager" />
	</property>
</bean>
```

## Common Issues and Solutions

| Issue | Solution |
|-------|----------|
| Skill not appearing in catalog | Check XML file name matches convention, verify `bean` attribute matches `id` |
| "Bean not found" error | Verify plugin.xml bean ID matches context usage, check class path spelling |
| XML parsing errors | Use XML validator, ensure special characters are escaped, check CDATA sections |
| Skill doesn't execute | Verify class extends `BaseSkill`, implements `process()`, calls `super.process()` |
| Missing moduleManager | Add property injection in plugin.xml bean definition |

## File Validation

### Validate Java Class
- File ends with `.java`
- Contains valid Java syntax
- Extends `BaseSkill`
- Has `@Override public void process(AgentContext)`

### Validate XML Files
- Well-formed XML (use VS Code XML validator)
- All special characters escaped (`&`, `<`, `>`, `"`, `'`)
- CDATA sections for text content containing special chars
- Proper closing tags

### Validate plugin.xml
- Run Maven or build system
- Spring context loads without errors
- All bean references resolve

## Next Steps After Creation

1. **Build/Compile**: Ensure Java class compiles
2. **Restart Server**: Restart application to load new beans
3. **Test in UI**: Navigate to catalog and verify skill appears
4. **Test Execution**: Create test agent context and execute skill
5. **Document**: Add usage documentation if needed

## References

- **Base Skill Class**: `/plugins/finder/code/org/entermediadb/ai/BaseSkill.java`
- **Example Skills**: `/plugins/finder/code/org/entermediadb/ai/skills/` (various implementations)
- **Catalog Structure**: `/plugins/catalog/html/data/lists/aiskill/` (existing XML files)
- **Plugin Configuration**: `/plugins/finder/html/src/plugin.xml`

## Support

For questions about:
- **Skill implementation**: Review existing skill classes in the skills folder
- **XML format**: Check other aiskill XML files for examples
- **Spring configuration**: Refer to plugin.xml bean definitions
- **AgentContext usage**: Review BaseSkill and AgentContext classes
