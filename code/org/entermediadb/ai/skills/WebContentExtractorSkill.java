package org.entermediadb.ai.skills;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.llm.BasicLlmResponse;
import org.json.simple.JSONObject;
import org.openedit.Data;

/**
 * WebContentExtractorSkill - Extracts and reads content from specified websites.
 * 
 * This skill is designed to: - Make HTTP requests to fetch web pages - Parse HTML content - Extract
 * text and metadata from web pages - Handle various web page types (HTML, text-based content) -
 * Support web scraping and data collection tasks
 * 
 * Usage in AgentContext: - Set "websiteurl" for the URL to fetch (required) - Set "extractmetadata"
 * (optional, boolean) to extract page metadata - Set "maxcontentlength" (optional, integer) to
 * limit content size - Set "requestmethod" (optional, string) for HTTP method (GET, POST, etc.),
 * defaults to GET - Set "useragent" (optional, string) for User-Agent header, defaults to standard
 * browser agent
 * 
 * Results stored in context: - "extractedcontent" - The extracted text content - "webpagemetadata"
 * - Metadata about the page (title, description, etc.) - "extractedlinks" - Collection of URLs
 * found on the page - "extractionstatus" - Status of the extraction operation (success/error) -
 * "extractiondate" - Timestamp of when extraction occurred - "errormessage" - Error details if
 * extraction failed
 */
public class WebContentExtractorSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(WebContentExtractorSkill.class);

	// Default HTTP request timeout (milliseconds)
	private static final int CONNECT_TIMEOUT = 10000;
	private static final int READ_TIMEOUT = 10000;

	// Default maximum content length (10 MB)
	private static final long DEFAULT_MAX_CONTENT_LENGTH = 10 * 1024 * 1024;

	// Default HTTP request settings
	private static final String DEFAULT_REQUEST_METHOD = "GET";
	private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

	// HTML tag patterns for extraction
	private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>.*?</script>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
	private static final Pattern STYLE_PATTERN = Pattern.compile("<style[^>]*>.*?</style>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
	private static final Pattern COMMENT_PATTERN = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
	private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>", Pattern.DOTALL);
	private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

	@Override
	public void process(AgentContext inContext)
	{
		String websiteUrl = (String) inContext.getContextValue("websiteurl");
		Boolean extractMetadata = (Boolean) inContext.getContextValue("extractmetadata");
		Object maxLengthObj = inContext.getContextValue("maxcontentlength");
		String requestMethod = (String) inContext.getContextValue("requestmethod");
		String userAgent = (String) inContext.getContextValue("useragent");

		if (websiteUrl == null || websiteUrl.trim().isEmpty())
		{
			log.warn("WebContentExtractorSkill: No website URL provided in context");
			inContext.putContextValue("extractionstatus", "error");
			inContext.putContextValue("errormessage", "No website URL provided");
			super.process(inContext);
			return;
		}

		try
		{
			long maxContentLength = DEFAULT_MAX_CONTENT_LENGTH;
			if (maxLengthObj != null)
			{
				maxContentLength = Long.parseLong(maxLengthObj.toString());
			}

			if (requestMethod == null || requestMethod.trim().isEmpty())
			{
				requestMethod = DEFAULT_REQUEST_METHOD;
			}

			if (userAgent == null || userAgent.trim().isEmpty())
			{
				userAgent = DEFAULT_USER_AGENT;
			}

			log.info("WebContentExtractorSkill: Extracting content from URL: " + websiteUrl);

			// Fetch HTML content
			String htmlContent = fetchWebContent(websiteUrl, maxContentLength, requestMethod, userAgent);

			if (htmlContent == null || htmlContent.trim().isEmpty())
			{
				log.warn("WebContentExtractorSkill: No content received from URL: " + websiteUrl);
				inContext.putContextValue("extractionstatus", "error");
				inContext.putContextValue("errormessage", "No content received from URL");
				super.process(inContext);
				return;
			}

			// Extract text content from HTML
			String extractedText = extractTextFromHtml(htmlContent);
			inContext.putContextValue("extractedcontent", extractedText);

			// Extract metadata if requested
			if (extractMetadata != null && extractMetadata.booleanValue())
			{
				Map<String, String> metadata = extractMetadata(htmlContent, websiteUrl);
				inContext.putContextValue("webpagemetadata", metadata);
				log.info("WebContentExtractorSkill: Metadata extracted - Title: " + metadata.get("title"));
			}

			// Extract links if needed
			Collection<String> links = extractLinks(htmlContent);
			inContext.putContextValue("extractedlinks", links);

			inContext.putContextValue("extractionstatus", "success");
			inContext.putContextValue("extractiondate", new Date());

			log.info("WebContentExtractorSkill: Successfully extracted " + extractedText.length() + " characters from " + websiteUrl);
		}
		catch (Exception e)
		{
			log.error("WebContentExtractorSkill: Error extracting content from " + websiteUrl, e);
			inContext.putContextValue("extractionstatus", "error");
			inContext.putContextValue("errormessage", e.getMessage());
		}

		super.process(inContext);
	}

	/**
	 * Fetches web content from the specified URL
	 * 
	 * @param urlString The URL to fetch
	 * @param maxContentLength Maximum content length to fetch
	 * @param requestMethod HTTP method to use (GET, POST, etc.)
	 * @param userAgent User-Agent header value
	 * @return HTML content as string, or null if unable to fetch
	 * @throws IOException If connection or reading fails
	 */
	private String fetchWebContent(String urlString, long maxContentLength, String requestMethod, String userAgent) throws IOException
	{
		// Validate and normalize URL
		if (!urlString.startsWith("http://") && !urlString.startsWith("https://"))
		{
			urlString = "https://" + urlString;
		}

		URL url = new URL(urlString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		// Set timeouts and headers
		connection.setConnectTimeout(CONNECT_TIMEOUT);
		connection.setReadTimeout(READ_TIMEOUT);
		connection.setRequestMethod(requestMethod);
		connection.setRequestProperty("User-Agent", userAgent);
		connection.setInstanceFollowRedirects(true);

		try
		{
			int responseCode = connection.getResponseCode();

			if (responseCode < 200 || responseCode >= 400)
			{
				log.warn("WebContentExtractorSkill: HTTP error " + responseCode + " for URL: " + urlString);
				return null;
			}

			StringBuilder contentBuilder = new StringBuilder();
			long totalRead = 0;

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)))
			{
				String line;
				while ((line = reader.readLine()) != null && totalRead < maxContentLength)
				{
					contentBuilder.append(line).append("\n");
					totalRead += line.length() + 1;
				}
			}

			return contentBuilder.toString();
		}
		finally
		{
			connection.disconnect();
		}
	}

	/**
	 * Extracts plain text content from HTML
	 * 
	 * @param htmlContent Raw HTML content
	 * @return Extracted text with basic formatting preserved
	 */
	private String extractTextFromHtml(String htmlContent)
	{
		if (htmlContent == null || htmlContent.trim().isEmpty())
		{
			return "";
		}

		String text = htmlContent;

		// Remove script and style tags
		text = SCRIPT_PATTERN.matcher(text).replaceAll(" ");
		text = STYLE_PATTERN.matcher(text).replaceAll(" ");
		text = COMMENT_PATTERN.matcher(text).replaceAll(" ");

		// Handle line breaks and paragraphs
		text = text.replaceAll("(?i)</p>", "\n");
		text = text.replaceAll("(?i)</br>", "\n");
		text = text.replaceAll("(?i)</div>", "\n");
		text = text.replaceAll("(?i)</li>", "\n");
		text = text.replaceAll("(?i)<li>", "• ");
		text = text.replaceAll("(?i)<h[1-6][^>]*>", "\n\n");
		text = text.replaceAll("(?i)</h[1-6]>", "\n\n");

		// Remove all remaining HTML tags
		text = TAG_PATTERN.matcher(text).replaceAll("");

		// Decode HTML entities
		text = decodeHtmlEntities(text);

		// Normalize whitespace
		text = WHITESPACE_PATTERN.matcher(text).replaceAll(" ");
		text = text.trim();

		return text;
	}

	/**
	 * Extracts metadata from HTML content
	 * 
	 * @param htmlContent Raw HTML content
	 * @param urlString The original URL
	 * @return Map containing page metadata
	 */
	private Map<String, String> extractMetadata(String htmlContent, String urlString)
	{
		Map<String, String> metadata = new HashMap<>();

		try
		{
			// Extract page title
			Pattern titlePattern = Pattern.compile("<title[^>]*>([^<]+)</title>", Pattern.CASE_INSENSITIVE);
			Matcher titleMatcher = titlePattern.matcher(htmlContent);
			if (titleMatcher.find())
			{
				metadata.put("title", titleMatcher.group(1).trim());
			}

			// Extract meta description
			Pattern descPattern = Pattern.compile("<meta\\s+name=['\"]description['\"]\\s+content=['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
			Matcher descMatcher = descPattern.matcher(htmlContent);
			if (descMatcher.find())
			{
				metadata.put("description", descMatcher.group(1).trim());
			}

			// Extract meta keywords
			Pattern keywordsPattern = Pattern.compile("<meta\\s+name=['\"]keywords['\"]\\s+content=['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
			Matcher keywordsMatcher = keywordsPattern.matcher(htmlContent);
			if (keywordsMatcher.find())
			{
				metadata.put("keywords", keywordsMatcher.group(1).trim());
			}

			// Extract page language
			Pattern langPattern = Pattern.compile("<html[^>]*lang=['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
			Matcher langMatcher = langPattern.matcher(htmlContent);
			if (langMatcher.find())
			{
				metadata.put("language", langMatcher.group(1).trim());
			}

			// Extract canonical URL
			Pattern canonicalPattern = Pattern.compile("<link[^>]*rel=['\"]canonical['\"][^>]*href=['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
			Matcher canonicalMatcher = canonicalPattern.matcher(htmlContent);
			if (canonicalMatcher.find())
			{
				metadata.put("canonical", canonicalMatcher.group(1).trim());
			}

			// Extract Open Graph title
			Pattern ogTitlePattern = Pattern.compile("<meta\\s+property=['\"]og:title['\"]\\s+content=['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
			Matcher ogTitleMatcher = ogTitlePattern.matcher(htmlContent);
			if (ogTitleMatcher.find())
			{
				metadata.put("og_title", ogTitleMatcher.group(1).trim());
			}

			// Extract Open Graph image
			Pattern ogImagePattern = Pattern.compile("<meta\\s+property=['\"]og:image['\"]\\s+content=['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
			Matcher ogImageMatcher = ogImagePattern.matcher(htmlContent);
			if (ogImageMatcher.find())
			{
				metadata.put("og_image", ogImageMatcher.group(1).trim());
			}

			metadata.put("url", urlString);
			metadata.put("extraction_timestamp", String.valueOf(System.currentTimeMillis()));
		}
		catch (Exception e)
		{
			log.warn("WebContentExtractorSkill: Error extracting metadata", e);
		}

		return metadata;
	}

	/**
	 * Extracts all hyperlinks from HTML content
	 * 
	 * @param htmlContent Raw HTML content
	 * @return Collection of extracted URLs
	 */
	private Collection<String> extractLinks(String htmlContent)
	{
		Collection<String> links = new ArrayList<>();

		try
		{
			// Pattern to match href attributes in anchor tags
			Pattern linkPattern = Pattern.compile("<a[^>]+href=['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
			Matcher linkMatcher = linkPattern.matcher(htmlContent);

			while (linkMatcher.find())
			{
				String link = linkMatcher.group(1).trim();
				if (!link.isEmpty() && !link.startsWith("#") && !link.startsWith("javascript:"))
				{
					links.add(link);
				}
			}
		}
		catch (Exception e)
		{
			log.warn("WebContentExtractorSkill: Error extracting links", e);
		}

		return links;
	}

	/**
	 * Decodes common HTML entities
	 * 
	 * @param text Text with HTML entities
	 * @return Decoded text
	 */
	private String decodeHtmlEntities(String text)
	{
		if (text == null)
		{
			return "";
		}

		return text.replace("&nbsp;", " ")
			.replace("&lt;", "<")
			.replace("&gt;", ">")
			.replace("&quot;", "\"")
			.replace("&apos;", "'")
			.replace("&amp;", "&")
			.replace("&copy;", "©")
			.replace("&reg;", "®")
			.replace("&deg;", "°")
			.replace("&mdash;", "—")
			.replace("&ndash;", "–")
			.replace("&bull;", "•")
			.replace("&hellip;", "…")
			.replace("&euro;", "€")
			.replace("&pound;", "£")
			.replace("&yen;", "¥");
	}
}
