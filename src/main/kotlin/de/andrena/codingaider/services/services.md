# Aider Services Documentation

## Overview
The Aider Services module provides a set of services that facilitate the management of user interactions, file handling, token counting, and documentation discovery within the Coding Aider application. This module is designed to support various functionalities such as maintaining history, counting tokens in text, managing persistent files, and finding relevant documentation.

## Key Classes and Interfaces

### AiderHistoryService
- **Purpose**: Manages the input and chat history of the application.
- **Key Methods**:
  - `getInputHistory()`: Returns a list of input history entries with timestamps.
  - `getLastChatHistory()`: Retrieves the last chat history entry.
- **Implementation Details**: Utilizes local files to store history data, ensuring persistence across sessions.

### TokenCountService
- **Purpose**: Counts tokens in text and files.
- **Key Methods**:
  - `countTokensInText(text: String)`: Counts the number of tokens in the provided text.
  - `countTokensInFiles(files: List<FileData>)`: Counts tokens across multiple files.
- **Implementation Details**: Uses the `jtokkit` library for token counting, supporting various encoding models.

### PersistentFileService
- **Purpose**: Handles the loading and saving of persistent files.
- **Key Methods**:
  - `loadPersistentFiles()`: Loads persistent file data from a YAML context file.
  - `savePersistentFilesToContextFile()`: Saves the current state of persistent files to the context file.
  - `addFile(file: FileData)`: Adds a new file to the persistent list.
- **Implementation Details**: Employs Jackson for YAML processing and IntelliJ's `LocalFileSystem` for file operations.

### AiderDialogStateService
- **Purpose**: Maintains the state of the dialog interactions.
- **Key Methods**:
  - `saveState(...)`: Saves the current dialog state.
  - `getLastState()`: Retrieves the last saved dialog state.
- **Implementation Details**: Stores dialog states in memory, providing quick access to the last interaction state.

### AiderPlanService
- **Purpose**: Manages the creation and handling of plans for coding tasks.
- **Key Methods**:
  - `createAiderPlanSystemPrompt(commandData: CommandData)`: Generates a system prompt for creating a coding plan.
- **Implementation Details**: Integrates structured mode markers to facilitate plan and checklist management.

### DocumentationFinderService
- **Purpose**: Discovers relevant markdown documentation files in the project hierarchy.
- **Key Methods**:
  - `findDocumentationFiles(virtualFiles: Array<VirtualFile>)`: Finds documentation files related to given files.
  - `findDocumentationForFile(file: VirtualFile)`: Traverses up the directory tree to find markdown files.
- **Implementation Details**: Recursively searches parent directories for markdown files to provide context.

## Design Patterns
- **Singleton Pattern**: Each service class is implemented as a singleton, ensuring that only one instance of each service exists per project.

## Dependencies
- The services depend on the IntelliJ Platform SDK for project management and file handling.
- The `FileData` class is used across multiple services to represent file-related data.

## Data Flow
- The `AiderHistoryService` and `TokenCountService` interact with user inputs and outputs, while `PersistentFileService` manages the state of files.
- The `AiderDialogStateService` maintains the state of user interactions, which can be influenced by the outputs of the `AiderPlanService`.

## File Links
- [AiderHistoryService.kt](./AiderHistoryService.kt)
- [TokenCountService.kt](./TokenCountService.kt)
- [PersistentFileService.kt](./PersistentFileService.kt)
- [AiderDialogStateService.kt](./AiderDialogStateService.kt)
- [AiderPlanService.kt](./AiderPlanService.kt)
