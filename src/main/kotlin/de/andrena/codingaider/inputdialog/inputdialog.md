# Aider Module Documentation

## Overview
The Aider module provides a user interface for interacting with various coding aids, including file management and command execution. It allows users to input commands, manage files, and view context-related information.

## Key Classes

### AiderInputDialog
- **Purpose**: This class represents the main dialog for user input. It allows users to enter commands and manage settings.
- **Constructor**: 
  - `AiderInputDialog(Project project, List<FileData> files, String initialText, ApiKeyChecker apiKeyChecker)`
- **Key Methods**:
  - `createCenterPanel()`: Constructs the main panel of the dialog.
  - `getInputText()`: Retrieves the text entered by the user.
  - `isYesFlagChecked()`: Checks if the "yes" flag is selected.
  - `getLlm()`: Gets the selected language model.
  - `getAdditionalArgs()`: Retrieves additional arguments entered by the user.

### AiderContextView
- **Purpose**: This class manages the context view of files, allowing users to see and interact with files in the project.
- **Constructor**: 
  - `AiderContextView(Project project, List<FileData> allFiles, (String) -> Unit onFileNameSelected, () -> Unit onFilesChanged)`
- **Key Methods**:
  - `selectedFilesChanged()`: Updates the view when the selected files change.
  - `addOpenFilesToContext()`: Adds currently open files to the context view.
  - `removeSelectedFiles()`: Removes selected files from the context view.

### AiderCompletionProvider
- **Purpose**: This class provides code completion suggestions based on the context of the user's input.
- **Constructor**: 
  - `AiderCompletionProvider(Project project, List<FileData> files)`
- **Key Methods**:
  - `getItems(String prefix, boolean cached, CompletionParameters parameters)`: Returns a collection of completion items based on the input prefix.
  - `extractCompletions(Project project, List<FileData> files)`: Extracts completion suggestions from the provided files.

## Design Patterns
- **MVC (Model-View-Controller)**: The Aider module follows the MVC pattern, separating the user interface (AiderInputDialog, AiderContextView) from the business logic (AiderCompletionProvider).

## Dependencies
- **IntelliJ Platform SDK**: The module relies on the IntelliJ Platform SDK for UI components and project management.
- **FileData**: A data class representing file information, used across the module for file management.

## Data Flow
1. The user interacts with the `AiderInputDialog` to enter commands.
2. The `AiderContextView` displays the files available in the project.
3. The `AiderCompletionProvider` suggests completions based on the user's input and the files in the project.

## Links to Key Files
- [AiderInputDialog.kt](./AiderInputDialog.kt)
- [AiderContextView.kt](./AiderContextView.kt)
- [AiderCompletionProvider.kt](./AiderCompletionProvider.kt)
