# Output View Module Documentation

## Overview
The Output View module is responsible for displaying output in a user-friendly manner within the application. It provides a dialog interface for presenting markdown content and allows users to interact with the output through various controls.

## Key Classes

### Abortable
- **File**: [Abortable.kt](Abortable.kt)
- **Description**: This interface defines a contract for classes that can abort a command. It contains a single method:
  - `fun abortCommand()`: This method should be implemented to define the behavior when a command is aborted.

### MarkdownDialog
- **File**: [MarkdownDialog.kt](MarkdownDialog.kt)
- **Description**: This class represents a dialog that displays markdown content. It allows users to view output and provides options to close or keep the dialog open.
- **Constructor Parameters**:
  - `project: Project`: The current project context.
  - `initialTitle: String`: The title of the dialog.
  - `initialText: String`: The initial text to display in the dialog.
  - `onAbort: Abortable?`: An optional Abortable instance to handle abort actions.
- **Key Methods**:
  - `fun updateProgress(output: String, message: String)`: Updates the displayed output and title of the dialog.
  - `fun startAutoCloseTimer(autocloseDelay: Int)`: Starts a timer to automatically close the dialog after a specified delay.
  - `fun setProcessFinished()`: Marks the process as finished and updates the close button text.
  - `fun focus(delay: Long = 100)`: Brings the dialog to the front and requests focus.

## Design Patterns
- The module utilizes the **Observer** pattern through the `Abortable` interface, allowing different components to respond to abort commands.

## Dependencies
- The `MarkdownDialog` class depends on the `Project` class from the IntelliJ platform and the `Abortable` interface for handling abort actions.

## Data Flow
- The `MarkdownDialog` receives output data and updates its display accordingly. It can also trigger abort actions through the `Abortable` interface.

## Exceptional Implementation Details
- The `MarkdownDialog` class includes a timer for auto-closing the dialog, which can be configured through user settings.

- The `MarkdownDialog` uses `invokeLater` to ensure UI updates are performed on the Event Dispatch Thread, which is crucial for thread safety in Swing applications.

- The `MarkdownDialog` provides a `Keep Open` button that allows users to cancel the auto-close timer, offering flexibility in user interaction.

