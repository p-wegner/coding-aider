# Aider Input Dialog Module Documentation

## Overview
The Aider Input Dialog module is a part of the Coding Aider application, which provides a user interface for executing commands and managing files within a project. This module facilitates user interactions by allowing them to input commands, select files, and view contextual information about the files in the project.

## Key Classes

### 1. AiderContextView
- **Purpose**: Manages the display of files and their context within a tree structure. It allows users to select files and view their properties.
- **Key Methods**:
  - `selectedFilesChanged()`: Updates the view when the selected files change.
  - `toggleReadOnlyMode()`: Toggles the read-only status of selected files.
  - `addOpenFilesToContext()`: Adds currently open files to the context view.

### 2. AiderInputDialog
- **Purpose**: Represents the dialog where users can input commands and manage file selections.
- **Key Methods**:
  - `createCenterPanel()`: Constructs the main panel of the dialog, including input fields and context view.
  - `getInputText()`: Retrieves the text input by the user.
  - `restoreLastState()`: Restores the dialog to its last used state.

### 3. AiderCompletionProvider
- **Purpose**: Provides autocompletion suggestions for user input based on the context of the files in the project.
- **Key Methods**:
  - `getItems(prefix: String, cached: Boolean, parameters: CompletionParameters?)`: Returns a collection of completion items based on the provided prefix.
  - `extractCompletions(project: Project, files: List<FileData>)`: Extracts completion items from the specified files.

## Design Patterns
- **MVC (Model-View-Controller)**: The module follows the MVC pattern by separating the user interface (AiderInputDialog and AiderContextView) from the data handling (AiderCompletionProvider).

## Dependencies
The Aider Input Dialog module interacts with several other modules within the Coding Aider application. Below is a PlantUML diagram illustrating the dependencies:

