# Coding Aider Utils Module

## Overview
The Coding Aider Utils module provides a set of utility classes and functions that facilitate various operations related to file handling, Git interactions, and API key management. This module is designed to support the main functionalities of the Coding Aider application by providing reusable components.

## Key Classes and Interfaces

### 1. `FileTraversal`
- **Purpose**: Provides methods to traverse files and directories.
- **Key Methods**:
  - `traverseFilesOrDirectories(files: Array<VirtualFile>, isReadOnly: Boolean = false): List<FileData>`: Traverses the provided files or directories and returns a list of `FileData`.

### 2. `GitUtils`
- **Purpose**: Contains utility functions for interacting with Git repositories.
- **Key Methods**:
  - `getCurrentCommitHash(project: Project): String?`: Retrieves the current commit hash of the Git repository associated with the given project.
  - `openGitComparisonTool(project: Project, commitHash: String, afterAction: () -> Unit)`: Opens a Git comparison tool to show changes related to a specific commit.

### 3. `ApiKeyChecker`
- **Purpose**: Interface for checking the availability of API keys for various LLMs (Large Language Models).
- **Key Methods**:
  - `isApiKeyAvailableForLlm(llm: String): Boolean`: Checks if an API key is available for the specified LLM.
  - `getApiKeyForLlm(llm: String): String?`: Retrieves the API key associated with the specified LLM.

### 4. `DefaultApiKeyChecker`
- **Purpose**: Implementation of the `ApiKeyChecker` interface.
- **Key Methods**:
  - Implements all methods defined in the `ApiKeyChecker` interface, providing functionality to check and retrieve API keys.

### 5. `ApiKeyManager`
- **Purpose**: Manages the storage and retrieval of API keys using the IntelliJ credential store.
- **Key Methods**:
  - `saveApiKey(keyName: String, apiKey: String)`: Saves the specified API key.
  - `getApiKey(keyName: String): String?`: Retrieves the specified API key.

### 6. `FileRefresher`
- **Purpose**: Provides functionality to refresh files in the virtual file system.
- **Key Methods**:
  - `refreshFiles(files: Array<VirtualFile>, markdownDialog: MarkdownDialog? = null)`: Refreshes the specified files and optionally shows a markdown dialog.

### 7. `ReflectionUtils`
- **Purpose**: Provides utility functions for reflection-based operations on specific console views.
- **Key Methods**:
  - `getNodesMapFromBuildView(view: BuildTreeConsoleView): Map<*, *>?`: Retrieves the nodes map from a build view.
  - `getTestsMapFromConsoleView(view: SMTRunnerConsoleView): Map<*, *>?`: Retrieves the tests map from a test runner console view.

## Design Patterns
- **Singleton Pattern**: Used in utility classes like `FileTraversal`, `GitUtils`, `ApiKeyManager`, and `FileRefresher` to ensure a single instance is used throughout the application.

## Dependencies
- The module relies on IntelliJ Platform SDK for file handling and project management.
- It interacts with Git through the `git4idea` library.
- Uses reflection to access private fields in specific console views.

## Data Flow
- The `FileTraversal` class can be used to gather file data, which can then be processed by other components like `GitUtils` for version control operations.
- The `ApiKeyChecker` and `ApiKeyManager` work together to manage API keys, ensuring that the application can securely access necessary credentials.

## Links to Files
- [FileTraversal.kt](FileTraversal.kt)
- [GitUtils.kt](GitUtils.kt)
- [ApiKeyChecker.kt](ApiKeyChecker.kt)
- [ApiKeyManager.kt](ApiKeyManager.kt)
- [FileRefresher.kt](FileRefresher.kt)
- [ReflectionUtils.kt](ReflectionUtils.kt)
