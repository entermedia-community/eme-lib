package org.entermediadb.ai.skills;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.AgentContext;
import org.entermediadb.ai.BaseSkill;

/**
 * FileOperationSkill - Reads and writes text files with configurable options.
 * 
 * This skill is designed to: - Read text content from files - Write text content to files - Support
 * file path resolution and validation - Handle overwrite protection and file creation - Provide
 * detailed operation status and error messages
 * 
 * Usage in AgentContext for READING: - Set "fileoperationtype" to "read" - Set "filename"
 * (required) - Name of the file to read - Set "filepath" (optional) - Directory path; if not
 * provided, will be extracted from filename
 * 
 * Usage in AgentContext for WRITING: - Set "fileoperationtype" to "write" - Set "filename"
 * (required) - Name of the file to write - Set "filepath" (optional) - Directory path for the file
 * - Set "filecontent" (required) - Content to write to the file - Set "overwritefile" (optional,
 * boolean) - Allow overwriting existing files (default: false)
 * 
 * Results stored in context: - "fileoperationstatus" - Status of the operation (success/error) -
 * "fileoperationresult" - For read: file content; For write: confirmation message -
 * "fileoperationdate" - Timestamp of when operation occurred - "errormessage" - Error details if
 * operation failed - "filepath" - Full path used for the operation
 */
public class FileOperationSkill extends BaseSkill
{
	private static final Log log = LogFactory.getLog(FileOperationSkill.class);

	private static final String OPERATION_READ = "read";
	private static final String OPERATION_WRITE = "write";
	private static final String DEFAULT_ENCODING = "UTF-8";

	@Override
	public void process(AgentContext inContext)
	{
		String operationType = (String) inContext.getContextValue("fileoperationtype");
		String filename = (String) inContext.getContextValue("filename");

		if (filename == null || filename.trim().isEmpty())
		{
			log.warn("FileOperationSkill: No filename provided in context");
			inContext.putContextValue("fileoperationstatus", "error");
			inContext.putContextValue("errormessage", "No filename provided");
			super.process(inContext);
			return;
		}

		try
		{
			if (OPERATION_READ.equalsIgnoreCase(operationType))
			{
				handleReadOperation(inContext, filename);
			}
			else if (OPERATION_WRITE.equalsIgnoreCase(operationType))
			{
				handleWriteOperation(inContext, filename);
			}
			else
			{
				log.warn("FileOperationSkill: Unknown operation type: " + operationType);
				inContext.putContextValue("fileoperationstatus", "error");
				inContext.putContextValue("errormessage", "Unknown operation type. Use 'read' or 'write'");
			}
		}
		catch (Exception e)
		{
			log.error("FileOperationSkill: Error during file operation", e);
			inContext.putContextValue("fileoperationstatus", "error");
			inContext.putContextValue("errormessage", e.getMessage());
		}

		inContext.putContextValue("fileoperationdate", new Date());
		super.process(inContext);
	}

	/**
	 * Handles file read operation
	 * 
	 * @param inContext AgentContext containing read parameters
	 * @param filename Name of the file to read
	 * @throws IOException If file reading fails
	 */
	private void handleReadOperation(AgentContext inContext, String filename) throws IOException
	{
		String filepath = (String) inContext.getContextValue("filepath");

		// Resolve file path
		File file = resolveFilePath(filename, filepath, true);

		if (file == null || !file.exists())
		{
			log.warn("FileOperationSkill: File not found: " + filename);
			inContext.putContextValue("fileoperationstatus", "error");
			inContext.putContextValue("errormessage", "File not found: " + filename);
			return;
		}

		if (!file.isFile())
		{
			log.warn("FileOperationSkill: Path is not a file: " + file.getAbsolutePath());
			inContext.putContextValue("fileoperationstatus", "error");
			inContext.putContextValue("errormessage", "Path is not a valid file: " + file.getAbsolutePath());
			return;
		}

		// Read file content
		String content = readFileContent(file);
		inContext.putContextValue("fileoperationresult", content);
		inContext.putContextValue("filepath", file.getAbsolutePath());
		inContext.putContextValue("fileoperationstatus", "success");

		log.info("FileOperationSkill: Successfully read " + content.length() + " characters from " + file.getAbsolutePath());
	}

	/**
	 * Handles file write operation
	 * 
	 * @param inContext AgentContext containing write parameters
	 * @param filename Name of the file to write
	 * @throws IOException If file writing fails
	 */
	private void handleWriteOperation(AgentContext inContext, String filename) throws IOException
	{
		String filepath = (String) inContext.getContextValue("filepath");
		String content = (String) inContext.getContextValue("filecontent");
		Object overwriteObj = inContext.getContextValue("overwritefile");

		if (content == null)
		{
			log.warn("FileOperationSkill: No content provided for write operation");
			inContext.putContextValue("fileoperationstatus", "error");
			inContext.putContextValue("errormessage", "No file content provided for write operation");
			return;
		}

		boolean overwrite = false;
		if (overwriteObj != null)
		{
			overwrite = Boolean.parseBoolean(overwriteObj.toString());
		}

		// Resolve file path
		File file = resolveFilePath(filename, filepath, false);

		if (file == null)
		{
			log.warn("FileOperationSkill: Unable to resolve file path for: " + filename);
			inContext.putContextValue("fileoperationstatus", "error");
			inContext.putContextValue("errormessage", "Unable to resolve file path for: " + filename);
			return;
		}

		// Check if file exists and overwrite is not allowed
		if (file.exists() && !overwrite)
		{
			log.warn("FileOperationSkill: File already exists and overwrite is not allowed: " + file.getAbsolutePath());
			inContext.putContextValue("fileoperationstatus", "error");
			inContext.putContextValue("errormessage", "File already exists. Set 'overwritefile' to true to overwrite");
			return;
		}

		// Create parent directories if they don't exist
		File parentDir = file.getParentFile();
		if (parentDir != null && !parentDir.exists())
		{
			if (!parentDir.mkdirs())
			{
				log.warn("FileOperationSkill: Failed to create parent directories: " + parentDir.getAbsolutePath());
				inContext.putContextValue("fileoperationstatus", "error");
				inContext.putContextValue("errormessage", "Failed to create parent directories");
				return;
			}
		}

		// Write file content
		writeFileContent(file, content);
		inContext.putContextValue("fileoperationresult", "File successfully written: " + file.getAbsolutePath());
		inContext.putContextValue("filepath", file.getAbsolutePath());
		inContext.putContextValue("fileoperationstatus", "success");

		log.info("FileOperationSkill: Successfully wrote " + content.length() + " characters to " + file.getAbsolutePath());
	}

	/**
	 * Resolves the complete file path from filename and optional filepath
	 * 
	 * @param filename Name of the file
	 * @param filepath Optional directory path
	 * @param isReadOperation True if this is a read operation
	 * @return Resolved File object, or null if unable to resolve
	 */
	private File resolveFilePath(String filename, String filepath, boolean isReadOperation)
	{
		File file;

		if (filepath != null && !filepath.trim().isEmpty())
		{
			// Use provided path
			file = new File(filepath, filename);
		}
		else if (filename.contains(File.separator) || filename.contains("/"))
		{
			// Extract path from filename
			file = new File(filename);
		}
		else
		{
			// No path information available
			if (isReadOperation)
			{
				// For read, search in current directory
				file = new File(filename);
			}
			else
			{
				// For write, use current directory
				file = new File(filename);
			}
		}

		return file;
	}

	/**
	 * Reads text content from a file
	 * 
	 * @param file File to read from
	 * @return File content as string
	 * @throws IOException If reading fails
	 */
	private String readFileContent(File file) throws IOException
	{
		StringBuilder contentBuilder = new StringBuilder();

		try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				contentBuilder.append(line).append("\n");
			}
		}

		// Remove trailing newline if present
		if (contentBuilder.length() > 0 && contentBuilder.charAt(contentBuilder.length() - 1) == '\n')
		{
			contentBuilder.setLength(contentBuilder.length() - 1);
		}

		return contentBuilder.toString();
	}

	/**
	 * Writes text content to a file
	 * 
	 * @param file File to write to
	 * @param content Content to write
	 * @throws IOException If writing fails
	 */
	private void writeFileContent(File file, String content) throws IOException
	{
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8)))
		{
			writer.write(content);
			writer.flush();
		}
	}
}
