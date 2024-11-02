# Input Dialog Module Documentation

This module is part of the `coding-aider` project and is responsible for providing user input dialog functionalities. It includes classes that handle user interactions, file context management, and auto-completion features within the dialog interface.

## Overview

The module consists of the following key components:

1. **AiderInputDialog**: A dialog wrapper that provides a user interface for entering commands and managing file contexts.
2. **AiderContextView**: A panel that displays and manages the context of files, allowing users to add, remove, and toggle file properties.
3. **AiderCompletionProvider**: Provides auto-completion suggestions for class and method names based on the project's files.

### AiderInputDialog

- **Purpose**: To create a dialog interface for user input, allowing users to enter commands, select files, and configure settings.
- **Key Features**:
  - Supports structured and shell modes.
  - Provides a history of previous commands.
  - Allows adding files to the context and toggling file properties.
  - Integrates with `AiderContextView` for file management.
  - Utilizes `AiderCompletionProvider` for auto-completion.
- **Public Interfaces**:
  - `getInputText()`: Returns the text input by the user.
  - `isYesFlagChecked()`: Checks if the "yes" flag is selected.
  - `getLlm()`: Retrieves the selected language model.
  - `getAdditionalArgs()`: Gets additional command arguments.
  - `getAllFiles()`: Returns all files in the current context.
  - `isShellMode()`: Checks if shell mode is enabled.
  - `isStructuredMode()`: Checks if structured mode is enabled.

### AiderContextView

- **Purpose**: To manage and display the context of files within the dialog.
- **Key Features**:
  - Displays files in a tree structure, categorized into regular files and markdown documents.
  - Allows users to add open files, toggle read-only and persistent states, and remove files.
  - Integrates with `PersistentFileService` to manage persistent file states.
- **Public Interfaces**:
  - `getOpenFiles()`: Retrieves currently open files.
  - `addOpenFilesToContext()`: Adds open files to the context.
  - `toggleReadOnlyMode()`: Toggles the read-only state of selected files.
  - `togglePersistentFile()`: Toggles the persistent state of selected files.
  - `removeSelectedFiles()`: Removes selected files from the context.
  - `addFilesToContext(fileDataList: List<FileData>)`: Adds specified files to the context.
  - `setFiles(files: List<FileData>)`: Sets the files in the context.

### AiderCompletionProvider

- **Purpose**: To provide auto-completion suggestions for class and method names.
- **Key Features**:
  - Extracts class and method names from project files.
  - Suggests completions based on user input prefix.
  - Utilizes IntelliJ's `TextFieldWithAutoCompletionListProvider` for integration.
- **Public Interfaces**:
  - `getItems(prefix: String, cached: Boolean, parameters: CompletionParameters?)`: Provides completion items based on the prefix.
  - `getLookupString(item: String)`: Returns the lookup string for a completion item.

## Dependencies

- **IntelliJ Platform**: The module relies on IntelliJ's APIs for UI components, file management, and project integration.
- **PersistentFileService**: Manages the persistence of file states across sessions.
- **TokenCountService**: Provides token counting functionality for files and text inputs.

## Design Patterns

- **Observer Pattern**: Used in `AiderContextView` to notify changes in file selection and context.
- **Factory Pattern**: Utilized in `AiderCompletionProvider` to create completion items based on project files.

## Integration Points

- The module interacts with other parts of the `coding-aider` system through services like `PersistentFileService` and `TokenCountService`.
- It provides a user interface for command input and file management, which is central to the `coding-aider` functionality.

This documentation provides a high-level overview of the input dialog module, detailing its components, features, and integration within the larger system.
