# Coding Aider Services Documentation

This document provides an overview of the key service classes within the `de.andrena.codingaider.services` package. Each service plays a specific role in the Coding Aider plugin, facilitating various functionalities such as dialog state management, history tracking, plan creation, token counting, and persistent file handling.

## AiderDialogStateService

### Purpose
The `AiderDialogStateService` is responsible for managing the state of dialogs within the Coding Aider plugin. It allows saving and retrieving the last known state of a dialog, which includes various parameters like message, flags, and file data.

### Key Classes and Methods
- **DialogState**: A data class that encapsulates the state of a dialog.
- **saveState(...)**: Saves the current state of a dialog.
- **getLastState()**: Retrieves the last saved state of a dialog.

### Integration
This service is used to maintain consistency in dialog interactions, ensuring that the state can be restored if needed.

## AiderHistoryService

### Purpose
The `AiderHistoryService` tracks the history of user inputs and chat interactions. It reads from and writes to history files, allowing users to review past commands and chat sessions.

### Key Classes and Methods
- **getInputHistory()**: Retrieves a list of past input commands with timestamps.
- **getLastChatHistory()**: Retrieves the most recent chat history.

### Integration
This service interacts with history files stored in the project directory, providing a way to persist and access historical data.

## AiderPlanService

### Purpose
The `AiderPlanService` manages the creation and handling of coding plans and checklists. It ensures that plans are documented and tracked properly within the project.

### Key Classes and Methods
- **createAiderPlanSystemPrompt(...)**: Generates a system prompt for creating or continuing a coding plan.

### Integration
This service uses markdown files to document plans and checklists, ensuring that development tasks are well-organized and tracked.

## TokenCountService

### Purpose
The `TokenCountService` provides functionality to count tokens in text and files, which is essential for processing and analyzing text data.

### Key Classes and Methods
- **countTokensInText(...)**: Counts the number of tokens in a given text.
- **countTokensInFiles(...)**: Counts tokens across multiple files.

### Integration
This service utilizes the `jtokkit` library to perform token counting, which is crucial for tasks that require text analysis.

## PersistentFileService

### Purpose
The `PersistentFileService` manages a list of persistent files within the project, allowing for the addition, removal, and updating of file data.

### Key Classes and Methods
- **loadPersistentFiles()**: Loads persistent files from a context file.
- **savePersistentFilesToContextFile()**: Saves the current list of persistent files to a context file.
- **addFile(...)**: Adds a new file to the persistent list.
- **removeFile(...)**: Removes a file from the persistent list.

### Integration
This service ensures that file data is consistently managed and stored, providing a reliable way to track files that are important to the project.

## Exceptional Implementation Details
- **AiderPlanService**: Utilizes a structured mode marker to differentiate between structured and unstructured plans.
- **PersistentFileService**: Uses Jackson for YAML processing, ensuring compatibility with Kotlin data classes.

This documentation provides a high-level overview of the services and their roles within the Coding Aider plugin, facilitating better understanding and maintenance of the codebase.
