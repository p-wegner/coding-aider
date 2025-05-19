# Web Crawling Functionality in Aider

## Overview

The Aider Web Crawl functionality provides a powerful tool for developers to extract, process, and utilize web content within their development workflow. This feature is implemented through the `AiderWebCrawlAction` class, which enables users to crawl web pages, convert HTML content to markdown, and process the resulting documentation for further use with the Aider AI assistant.

## Key Components

### AiderWebCrawlAction

The `AiderWebCrawlAction` class is an IntelliJ IDEA action that provides web crawling capabilities integrated with the Aider AI assistant. It allows users to:

- Input a URL to crawl
- Automatically extract and process web page content
- Convert HTML to clean markdown format
- Store the processed content in a structured file hierarchy
- Optionally process the content further using AI assistance

#### Implementation Details

```kotlin
class AiderWebCrawlAction : AnAction() {
    // Action implementation that handles web crawling and processing
}
```

#### Key Methods

- `actionPerformed(e: AnActionEvent)`: Main entry point that handles the user interaction flow
- `crawlAndProcessWebPage(url: String, file: File, project: Project)`: Performs the actual web crawling and HTML-to-markdown conversion
- `refreshAndAddFile(project: Project, filePath: String)`: Refreshes the file system and adds the generated file to persistent files
- `showNotification(project: Project, content: String, type: NotificationType)`: Displays notifications to the user

## Workflow

1. **URL Input**: The user is prompted to enter a URL to crawl
2. **File Path Generation**: 
   - Creates a directory structure based on the domain name
   - Generates a unique filename using MD5 hash of the URL
   - Checks if the file already exists to prevent duplicate processing

3. **Web Crawling**:
   - Uses HtmlUnit's WebClient to fetch the web page
   - Disables JavaScript, CSS, and error exceptions for reliable crawling
   - Extracts the HTML content from the page

4. **Markdown Conversion**:
   - Converts the HTML content to markdown format
   - Preserves the structure and content while simplifying the format
   - Stores metadata including the original URL

5. **AI Processing** (Optional):
   - If enabled, sends the raw markdown to the Aider AI assistant
   - Provides specific instructions for cleaning and simplifying the content
   - Focuses on removing navigation elements, advertisements, and simplifying structure

6. **File Management**:
   - Refreshes the file system to recognize the new file
   - Adds the file to the PersistentFileService for future reference
   - Notifies the user about the completed process

## Exceptional Implementation Details

### Configurable HTML Processing

The web client is configured to disable potentially problematic features:

```kotlin
webClient.options.apply {
    isJavaScriptEnabled = false
    isThrowExceptionOnScriptError = false
    isThrowExceptionOnFailingStatusCode = false
    isCssEnabled = false
}
```

This ensures reliable crawling across different types of websites by avoiding common issues with JavaScript execution and CSS rendering.

### Unique File Naming Strategy

The implementation uses a sophisticated file naming strategy that combines:

1. The page name from the URL path
2. An MD5 hash of the full URL to ensure uniqueness
3. A "raw" infix to indicate unprocessed content

```kotlin
val combinedHash = MessageDigest.getInstance("MD5").digest(url.toByteArray()).let {
    BigInteger(1, it).toString(16).padStart(32, '0')
}

val pageName = URI(url).toURL().path.split("/").lastOrNull() ?: "index"
val fileName = "$pageName-raw-$combinedHash.md"
```

This approach prevents file name collisions while maintaining human-readable names.

### AI-Powered Content Processing

The action includes detailed instructions for the AI to process the raw web content:

```kotlin
message = """
    Clean up and simplify the provided file $fileName. Follow these guidelines:
    1. Remove all navigation elements, headers, footers, and sidebars.
    2. Delete any advertisements, banners, or promotional content.
    ...
```

These instructions guide the AI to transform raw web content into clean, focused documentation by removing irrelevant elements while preserving the valuable technical information.

### Integration with Persistent File System

The action automatically adds the generated file to the persistent file service:

```kotlin
val persistentFileService = project.getService(PersistentFileService::class.java)
persistentFileService.addFile(FileData(filePath, true))
```

This ensures that the crawled content remains available for future Aider operations without requiring manual file selection.

## Configuration Options

The web crawling functionality includes several configurable options:

- **LLM Selection**: Users can specify which language model to use for processing the crawled content
- **IDE Executor Activation**: Option to automatically process the content with the IDE-based executor after crawling
- **File Organization**: Content is automatically organized in a structured folder hierarchy

## Integration with Aider Actions Module

The Web Crawl action is part of the broader Aider Actions module, which provides a comprehensive set of IntelliJ IDEA actions that integrate with the Aider AI assistant. This module serves as the primary interface between the user and the Aider functionality.

### Related Components

- **MarkdownConversionService**: Handles the conversion of HTML to markdown
- **PersistentFileService**: Manages files that should be persistently available to Aider
- **IDEBasedExecutor**: Executes Aider commands within the IDE environment
- **FileRefresher**: Ensures the IDE recognizes newly created files

## Use Cases

The Web Crawl functionality is particularly useful for:

1. **Documentation Integration**: Incorporating external documentation into project documentation
2. **API Reference**: Extracting API references from web-based documentation
3. **Research Collection**: Gathering research materials in a consistent format
4. **Knowledge Base Building**: Creating a local knowledge base from online resources
5. **Tutorial Adaptation**: Converting online tutorials to project-specific documentation

## Best Practices

When using the Web Crawl functionality:

1. **Respect Copyright**: Only crawl content you have permission to use
2. **Be Selective**: Target specific pages rather than crawling entire sites
3. **Process Results**: Review and edit the AI-processed content for accuracy
4. **Organize Content**: Use meaningful file names and folder structures
5. **Add Context**: Provide additional context to connect the crawled content to your project

## Technical Requirements

- IntelliJ IDEA with the Aider plugin installed
- HtmlUnit library for web page processing
- Internet connection for accessing web content
- Sufficient disk space for storing crawled content
