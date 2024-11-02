# Persistent Files Tool Window

## Overview
The `PersistentFilesToolWindow` module provides a user interface component for managing persistent files within the Coding Aider application. It allows users to add, remove, and toggle the read-only status of files, as well as view a list of persistent files.

## Key Classes and Interfaces

### PersistentFilesToolWindow
- **Purpose**: Implements the `ToolWindowFactory` interface to create the content of the persistent files tool window.
- **Methods**:
  - `createToolWindowContent(project: Project, toolWindow: ToolWindow)`: Initializes the tool window content.

### PersistentFilesComponent
- **Purpose**: Manages the list of persistent files and handles user interactions.
- **Methods**:
  - `getContent()`: Returns the main UI component for the tool window.
  - `addPersistentFiles()`: Opens a file chooser to add files to the persistent list.
  - `toggleReadOnlyMode()`: Toggles the read-only status of selected files.
  - `removeSelectedFiles()`: Removes selected files from the persistent list.
  - `loadPersistentFiles()`: Loads the current list of persistent files from the service.

### PersistentFileRenderer
- **Purpose**: Custom renderer for displaying file data in the list.
- **Methods**:
  - `getListCellRendererComponent(...)`: Customizes the display of each file in the list.

## Design Patterns
- **Observer Pattern**: The module subscribes to changes in persistent files using the message bus, allowing it to react to updates in real-time.

## Dependencies
- **PersistentFileService**: A service that manages the persistent file data.
- **FileData**: A data class representing the file information, including its path and read-only status.

## Data Flow
1. The `PersistentFilesToolWindow` creates an instance of `PersistentFilesComponent`.
2. `PersistentFilesComponent` interacts with `PersistentFileService` to manage file data.
3. User actions (add, remove, toggle) trigger updates in the UI and the underlying data model.

## Important Links
- [PersistentFilesToolWindow.kt](./PersistentFilesToolWindow.kt)
