# Command Module Documentation

## Overview
The Command module is responsible for defining the data structures and interfaces required to execute commands in the Aider application. It encapsulates the necessary information for command execution, including messages, flags, and file data.

## Key Classes

### FileData
- **Purpose**: Represents a file's metadata.
- **Properties**:
  - `filePath`: The path to the file.
  - `isReadOnly`: A boolean indicating if the file is read-only.

### CommandData
- **Purpose**: Represents the data required to execute an Aider command.
- **Properties**:
  - `message`: The main message or instruction for Aider.
  - `useYesFlag`: If true, automatically confirms all prompts.
  - `llm`: The language model to be used (e.g., "gpt-4").
  - `additionalArgs`: Any additional command-line arguments for Aider.
  - `files`: List of `FileData` instances to be included in the Aider command.
  - `isShellMode`: If true, enables shell mode for Aider.
  - `lintCmd`: Command to run for linting the code.
  - `deactivateRepoMap`: If true, disables the repository mapping feature.
  - `editFormat`: Specifies the format for edit instructions (e.g., "diff").
  - `projectPath`: The path to the project.
  - `options`: Contains optional parameters for the command, including whether to disable presentation of changes.
  - `structuredMode`: If true, enables structured mode.

### CommandOptions
- **Purpose**: Represents the optional parameters for the Aider command.
- **Properties**:
  - `disablePresentation`: If true, disables the presentation of changes after command execution.
  - `useDockerAider`: If true, uses the Docker Aider image ignoring the settings.
  - `commitHashToCompareWith`: If not null, compares the changes with the specified commit hash.
  - `autoCloseDelay`: If not null, sets the auto close delay for the markdown dialog.

## Dependencies
This module relies on the Kotlin standard library and is designed to work within the Aider application framework. The `FileData`, `CommandData`, and `CommandOptions` classes are used throughout the application to manage command execution.

## Data Flow
The `CommandData` class aggregates multiple `FileData` instances, allowing commands to be executed on multiple files simultaneously. The data flows from user input through the command interface, where it is processed and executed based on the properties defined in these classes.

## Design Patterns
The module follows the Data Transfer Object (DTO) pattern, encapsulating data in simple objects that can be easily transferred between different parts of the application.

## Links to Files
- [FileData.kt](./FileData.kt)
- [CommandData.kt](./CommandData.kt)
