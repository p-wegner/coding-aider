# PersistentFilesToolWindow Module Documentation

## Overview
The `PersistentFilesToolWindow` module is part of the Coding Aider project, designed to manage persistent files within the IntelliJ IDE. This module provides a user interface for users to add, remove, and toggle the read-only status of files that they wish to persist across sessions.

## Key Classes and Interfaces

### PersistentFilesToolWindow
- **Purpose**: Implements the `ToolWindowFactory` interface to create the content of the persistent files tool window.
- **Methods**:
  - `createToolWindowContent(project: Project, toolWindow: ToolWindow)`: Initializes the tool window with the persistent files component.

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
  - `getListCellRendererComponent(...)`: Customizes the display of each file in the list, indicating if it is read-only.

## Design Patterns
- **Observer Pattern**: The module subscribes to changes in persistent files using the message bus, allowing it to react to updates in real-time.

## Dependencies
- **PersistentFileService**: This service is responsible for managing the persistent files' data, including adding, updating, and removing files.
- **FileData**: A data class representing the file's path and its read-only status.

## Data Flow
1. The user interacts with the `PersistentFilesComponent` to add, remove, or toggle files.
2. The component communicates with the `PersistentFileService` to perform these actions.
3. The service updates the underlying data and notifies the component of any changes, which then refreshes the displayed list.

## Integration Points
- The module integrates with the IntelliJ platform through the `ToolWindowFactory` interface, allowing it to be part of the IDE's UI.
- It also interacts with the message bus for real-time updates on persistent files.

## PlantUML Diagram
