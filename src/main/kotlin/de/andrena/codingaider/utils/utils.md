# Coding Aider Utilities Module

## Overview
The Coding Aider Utilities module provides a set of utility classes and functions that facilitate interactions with Git repositories, manage API keys, refresh files in the IDE, and traverse file structures. This module is designed to support the overall functionality of the Coding Aider application by providing essential services that other modules can leverage.

## Key Classes and Their Responsibilities

### GitUtils
- **Purpose**: Provides utility functions for interacting with Git repositories.
- **Key Methods**:
  - `getCurrentCommitHash(project: Project): String?`: Retrieves the current commit hash of the Git repository associated with the given project.
  - `openGitComparisonTool(project: Project, commitHash: String, afterAction: () -> Unit)`: Opens a Git comparison tool to show changes related to a specific commit.
  - `findGitRoot(directory: File): File?`: Finds the root directory of a Git repository.

### ApiKeyManager
- **Purpose**: Manages API keys securely using the IDE's credential store.
- **Key Methods**:
  - `saveApiKey(keyName: String, apiKey: String)`: Saves an API key associated with a given name.
  - `getApiKey(keyName: String): String?`: Retrieves the API key for a given name.
  - `removeApiKey(keyName: String)`: Removes the API key associated with a given name.

### FileRefresher
- **Purpose**: Refreshes files in the IDE and optionally shows a markdown dialog.
- **Key Methods**:
  - `refreshFiles(files: Array<VirtualFile>, markdownDialog: MarkdownDialog? = null)`: Refreshes the specified files and makes the markdown dialog visible if provided.

### ReflectionUtils
- **Purpose**: Provides reflection utilities to access private fields in specific console views.
- **Key Methods**:
  - `getNodesMapFromBuildView(view: BuildTreeConsoleView): Map<*, *>?`: Retrieves the nodes map from a build view.
  - `getTestsMapFromConsoleView(view: SMTRunnerConsoleView): Map<*, *>?`: Retrieves the tests map from a test runner console view.

### ApiKeyChecker
- **Purpose**: Checks the availability of API keys for various LLMs (Large Language Models).
- **Key Methods**:
  - `isApiKeyAvailableForLlm(llm: String): Boolean`: Checks if an API key is available for a specific LLM.
  - `getApiKeyForLlm(llm: String): String?`: Retrieves the API key associated with a specific LLM.
  - `getApiKeysForDocker(): Map<String, String>`: Retrieves a map of API keys suitable for Docker usage.

### FileTraversal
- **Purpose**: Traverses files and directories to gather file data.
- **Key Methods**:
  - `traverseFilesOrDirectories(files: Array<VirtualFile>, isReadOnly: Boolean = false): List<FileData>`: Traverses the provided files or directories and returns a list of file data.

## Dependencies and Data Flow
The Coding Aider Utilities module interacts with various components of the IDE and other modules within the Coding Aider application. The following PlantUML diagram illustrates the dependencies between this module and others:

