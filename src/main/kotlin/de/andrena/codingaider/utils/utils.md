# Utils Module Documentation

This document provides an overview of the utility classes and functions available in the `de.andrena.codingaider.utils` package. These utilities are designed to support various functionalities across the Coding Aider project.

## GitUtils

### Overview
`GitUtils` provides utility functions for interacting with Git repositories within an IntelliJ project. It includes methods to retrieve the current commit hash and open a Git comparison tool.

### Key Methods
- `getCurrentCommitHash(project: Project): String?`: Retrieves the current commit hash of the given project.
- `openGitComparisonTool(project: Project, commitHash: String, afterAction: () -> Unit)`: Opens a comparison tool to show differences for a specific commit.

### Dependencies
- Relies on IntelliJ's Git integration and VCS (Version Control System) APIs.

## ApiKeyChecker

### Overview
`ApiKeyChecker` is an interface for checking the availability of API keys required for different LLM (Language Model) options. The `DefaultApiKeyChecker` class implements this interface.

### Key Methods
- `isApiKeyAvailableForLlm(llm: String): Boolean`: Checks if an API key is available for a given LLM.
- `isApiKeyAvailable(apiKeyName: String): Boolean`: Checks if a specific API key is available.
- `getApiKeyForLlm(llm: String): String?`: Retrieves the API key associated with a given LLM.
- `getAllLlmOptions(): List<String>`: Returns all available LLM options.
- `getAllApiKeyNames(): List<String>`: Returns all distinct API key names.
- `getApiKeyValue(apiKeyName: String): String?`: Retrieves the value of a specified API key.
- `getApiKeysForDocker(): Map<String, String>`: Retrieves API keys formatted for Docker usage.

### Implementation Details
- Supports multiple sources for API keys: CredentialStore, environment variables, and `.env` files.

## ApiKeyManager

### Overview
`ApiKeyManager` handles the storage and retrieval of API keys using IntelliJ's PasswordSafe.

### Key Methods
- `saveApiKey(keyName: String, apiKey: String)`: Saves an API key.
- `getApiKey(keyName: String): String?`: Retrieves an API key.
- `removeApiKey(keyName: String)`: Removes an API key.

### Dependencies
- Utilizes IntelliJ's PasswordSafe for secure storage.

## FileRefresher

### Overview
`FileRefresher` provides functionality to refresh virtual files in the IntelliJ environment, ensuring that file changes are recognized by the IDE.

### Key Methods
- `refreshFiles(files: Array<VirtualFile>, markdownDialog: MarkdownDialog? = null)`: Refreshes the specified files and optionally displays a markdown dialog.

### Dependencies
- Integrates with IntelliJ's VirtualFileManager and RefreshQueue.

## FileTraversal

### Overview
`FileTraversal` offers methods to traverse directories and files, returning metadata about each file.

### Key Methods
- `traverseFilesOrDirectories(files: Array<VirtualFile>, isReadOnly: Boolean = false): List<FileData>`: Traverses the given files or directories and returns a list of `FileData`.

### Dependencies
- Works with IntelliJ's VirtualFile system.

## ReflectionUtils

### Overview
`ReflectionUtils` provides utility methods for accessing private fields in classes using reflection, specifically for IntelliJ's console views.

### Key Methods
- `getNodesMapFromBuildView(view: BuildTreeConsoleView): Map<*, *>?`: Retrieves the nodes map from a build view.
- `getTestsMapFromConsoleView(view: SMTRunnerConsoleView): Map<*, *>?`: Retrieves the tests map from a console view.

### Implementation Details
- Uses Java reflection to access private fields, which may be subject to change in future IntelliJ versions.

## General Notes
- These utilities are tightly integrated with IntelliJ's API and are intended for use within the Coding Aider plugin.
- Proper error handling and logging are implemented to ensure robustness.
