# Coding Aider Services Documentation

This document provides an overview of the key services within the Coding Aider project, detailing their purpose, functionality, and integration within the system.

## AiderPlanService

### Overview
The `AiderPlanService` is responsible for managing the creation and maintenance of coding plans and checklists. It ensures that any feature requests are documented in a structured manner before implementation begins.

### Key Features
- **Plan and Checklist Management**: Creates and updates markdown files to track feature requests and implementation progress.
- **Integration with Project**: Utilizes IntelliJ's project service framework to manage plans at the project level.
- **Markers and Directories**: Uses specific markers and directories to organize and identify plans and checklists.

### Public Interface
- `createAiderPlanSystemPrompt(commandData: CommandData): String`: Generates a system prompt for creating or updating a plan based on existing files and new command data.

### Dependencies
- Relies on IntelliJ's project service framework.
- Interacts with markdown files in the `.coding-aider-plans` directory.

## TokenCountService

### Overview
The `TokenCountService` provides functionality to count tokens in text and files, which is essential for processing and analyzing text data.

### Key Features
- **Token Counting**: Uses a specified encoding to count tokens in given text or files.
- **Encoding Management**: Supports different encoding models, defaulting to GPT-4O.

### Public Interface
- `countTokensInText(text: String, encoding: Encoding = this.encoding): Int`: Counts tokens in a given text.
- `countTokensInFiles(files: List<FileData>, encoding: Encoding = this.encoding): Int`: Counts tokens across multiple files.

### Dependencies
- Utilizes the `jtokkit` library for encoding and token counting.

## AiderHistoryService

### Overview
The `AiderHistoryService` manages the history of inputs and chat interactions, providing a way to review past activities and decisions.

### Key Features
- **History Management**: Stores and retrieves input and chat history.
- **Date and Time Formatting**: Uses a specific format to timestamp entries.

### Public Interface
- `getInputHistory(): List<Pair<LocalDateTime, List<String>>>`: Retrieves a list of past input commands with timestamps.
- `getLastChatHistory(): String`: Fetches the most recent chat history.

### Dependencies
- Relies on file storage for history management.

## PersistentFileService

### Overview
The `PersistentFileService` handles the persistence of file data, ensuring that file states are maintained across sessions.

### Key Features
- **File Persistence**: Loads and saves file data to a context file.
- **Change Notification**: Notifies the system of changes to persistent files.

### Public Interface
- `loadPersistentFiles(): List<FileData>`: Loads persistent files from the context file.
- `savePersistentFilesToContextFile()`: Saves the current state of files to the context file.
- `addFile(file: FileData)`: Adds a file to the persistent list.
- `removeFile(filePath: String)`: Removes a file from the persistent list.

### Dependencies
- Uses Jackson for YAML processing.
- Integrates with IntelliJ's application and project services.

## AiderDialogStateService

### Overview
The `AiderDialogStateService` maintains the state of dialog interactions, allowing for the restoration of previous states.

### Key Features
- **State Management**: Saves and retrieves the last known state of dialog interactions.
- **Data Class for State**: Utilizes a data class to encapsulate dialog state information.

### Public Interface
- `saveState(...)`: Saves the current state of a dialog.
- `getLastState(): DialogState?`: Retrieves the last saved state.

### Dependencies
- Operates within the IntelliJ project service framework.

## Exceptional Implementation Details
- **AiderPlanService**: Utilizes a structured approach to manage plans and checklists, ensuring that all changes are documented before implementation.
- **PersistentFileService**: Employs a robust mechanism for file persistence, leveraging YAML for configuration management.

This documentation provides a comprehensive overview of the services, aiding developers in understanding their roles and interactions within the Coding Aider system.
