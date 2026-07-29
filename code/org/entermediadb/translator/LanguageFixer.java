package org.entermediadb.translator;

import java.util.Collection;
import org.dom4j.Element;
import org.openedit.Data;
import org.openedit.modules.translations.Translator;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.ContentItem;
import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

public class LanguageFixer
{

	PageManager fieldPageManager;

	Collection<String> fieldPaths;
	XmlArchive fieldXmlArchive;

	Collection<Data> fieldLocales;

	Translator translator;

	public Translator getTranslator()
	{
		return translator;
	}

	public void setTranslator(Translator inTranslator)
	{
		translator = inTranslator;
	}

	public Collection<Data> getLocales()
	{
		return fieldLocales;
	}

	public void setLocales(Collection<Data> inLocales)
	{
		fieldLocales = inLocales;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public Collection<String> getPaths()
	{
		return fieldPaths;
	}

	public void setPaths(Collection<String> inPaths)
	{
		fieldPaths = inPaths;
	}

	public XmlArchive getXmlArchive()
	{
		return fieldXmlArchive;
	}

	public void setXmlArchive(XmlArchive inXmlArchive)
	{
		fieldXmlArchive = inXmlArchive;
	}

	public void translateNames()
	{

		for (String path : getPaths())
		{
			translateNames(path);
		}
	}

	public void translateNames(String path)
	{
		ContentItem item = getPageManager().getRepository().getStub(path);
		if (item.isFolder())
		{
			Collection<String> children = getPageManager().getChildrenPaths(item.getPath());
			for (String child : children)
			{
				translateNames(child);
			}
			return;
		}

		XmlFile file = getXmlArchive().getXml(path);
		boolean changed = false;
		if (file != null)
		{
			for (Element row : (Collection<Element>) file.getElements())
			{
				Element name = row.element("name");
				if (name != null)
				{
					/**
					 * <name> <language id="en"><![CDATA[Uploading]]></language> </name>
					 */
					String english = name.elementText("language[@id='en']");
					if (english == null)
					{
						continue;
					}
					for (Data locale : getLocales())
					{
						String lang = locale.get("id");
						String value = name.elementText("language[@id='" + lang + "']");
						if (value == null)
						{
							String translated = getTranslator().webTranslate(english, "en", lang);
							Element newchild = name.addElement("language").addAttribute("id", lang);
							// Add CDATA to avoid issues with special characters
							newchild.addCDATA(translated);
							changed = true;
						}
					}
				}
			}
		}
		if (changed)
		{
			getXmlArchive().saveXml(file, null);
		}

	}
}
