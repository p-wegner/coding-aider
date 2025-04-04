# Coding Aider Tool Window

## Overview

The Coding Aider Tool Window is a specialized interface in the Coding Aider application that provides three main panels:

1. **Persistent Files Panel** - For managing frequently accessed files.
2. **Plans Panel** - For viewing and executing Aider plans.
3. **Working Directory Panel** - For restricting Aider operations to a specific directory.
4. **Running Commands Panel** - For monitoring running commands.

## Architecture and Design

### Key Components

- **CodingAiderToolWindow**: Main tool window factory implementation.
- **CodingAiderToolWindowContent**: Content manager for all panels.
- **PersistentFilesPanel**: Manages persistent file list.
- **PlansPanel**: Handles plan display and execution.
- **PlanViewer**: Renders and manages plan interactions.
- **RunningCommandsPanel**: Monitors running commands.

### Design Patterns

- **Observer Pattern**: Uses IntelliJ Platform's message bus for real-time updates.
- **Component-Based Design**: Separates UI and business logic.
- **Factory Pattern**: Tool window creation.

### Class Responsibilities

#### CodingAiderToolWindow

- Implements `ToolWindowFactory`.
- Creates and initializes tool window content.
- **Key Method**: `createToolWindowContent(project: Project, toolWindow: ToolWindow)`.

#### PersistentFilesPanel

Manages persistent files functionality:

- File list management.
- Read-only mode toggling.
- Batch file operations.
- Real-time file list updates.

**Key Features**:

- Add individual files or directories.
- Add currently open files.
- Toggle read-only status.
- Remove selected files.
- Double-click to open files.

#### PlansPanel & PlanViewer

Handles Aider plans:

- Displays plan list with completion status.
- Shows checklist progress.
- Enables plan continuation.
- Provides plan details on hover.

**Key Features**:

- Visual progress indicators.
- Plan execution controls.
- Double-click to open plan files.
- Real-time plan updates.

#### WorkingDirectoryPanel

Manages working directory configuration:
- Directory selection within project
- Path validation and normalization
- Clear/reset functionality
- Automatic subtree-only mode

**Key Features**:
- Visual directory path display
- Directory selection via file chooser
- Working directory persistence
- Project-relative path validation

#### RunningCommandsPanel

Monitors running commands:

- Displays a list of currently running commands.
- Double-click to focus on a running command.

### Data Flow

```mermaid
graph TD
    User[User Interaction] --> ToolWindow[Tool Window]
    ToolWindow --> PersistentFiles[Persistent Files Panel]
    ToolWindow --> Plans[Plans Panel]
    ToolWindow --> RunningCommands[Running Commands Panel]
    PersistentFiles --> FileService[PersistentFileService]
    Plans --> PlanService[AiderPlanService]
    RunningCommands --> RunningCommandService[RunningCommandService]
    FileService --> MessageBus[Message Bus]
    PlanService --> MessageBus
    RunningCommandService --> MessageBus
    MessageBus --> UI[UI Update]
```

### Implementation Details

- File system change monitoring.
- Plan completion tracking.
- Keyboard shortcuts support.
- Customizable UI components.

### Dependencies

- IntelliJ Platform SDK.
- Kotlin Stdlib.
- Custom services:
    - PersistentFileService.
    - AiderPlanService.
    - RunningCommandService.

### Usage Scenarios

1. Managing reference files across sessions.
2. Tracking and executing multi-step plans.
3. Monitoring plan progress.
4. Quick access to relevant project files.
5. Monitoring running commands.

## Configuration

The tool window is configurable through:

- IntelliJ IDE settings.
- Coding Aider plugin settings.

## Related Documentation

- [AiderPlan](../services/plans/AiderPlanService.kt)
- [PersistentFileService](../services/PersistentFileService.kt)
- [RunningCommandService](../services/RunningCommandService.kt)
