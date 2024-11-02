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

### AiderContextView

- **Purpose**: To manage and display the context of files within the dialog.
- **Key Features**:
  - Displays files in a tree structure, categorized into regular files and markdown documents.
  - Allows users to add open files, toggle read-only and persistent states, and remove files.
  - Integrates with `PersistentFileService` to manage persistent file states.

### AiderCompletionProvider

- **Purpose**: To provide auto-completion suggestions for class and method names.
- **Key Features**:
  - Extracts class and method names from project files.
  - Suggests completions based on user input prefix.
  - Utilizes IntelliJ's `TextFieldWithAutoCompletionListProvider` for integration.

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
