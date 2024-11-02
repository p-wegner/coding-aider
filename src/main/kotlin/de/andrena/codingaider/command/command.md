# Command Module Documentation

This module is part of the `de.andrena.codingaider.command` package and provides data structures for managing command execution within the Coding Aider system. It includes classes that encapsulate the necessary information for executing commands and handling file data.

## Overview

The module consists of two primary data classes:

1. **FileData**: Represents metadata about a file, including its path and read-only status.
2. **CommandData**: Encapsulates all the information required to execute a command, including message, language model, file list, and various flags.

### FileData Class

- **Purpose**: To store information about a file, specifically its path and whether it is read-only.
- **Attributes**:
  - `filePath`: A `String` representing the path to the file.
  - `isReadOnly`: A `Boolean` indicating if the file is read-only.

### CommandData Class

- **Purpose**: To encapsulate all necessary data for executing a command in the Coding Aider system.
- **Attributes**:
  - `message`: A `String` containing the main message or instruction for Aider.
  - `useYesFlag`: A `Boolean` that, if true, automatically confirms all prompts.
  - `llm`: A `String` specifying the language model to be used (e.g., "gpt-4").
  - `additionalArgs`: A `String` for any additional command-line arguments for Aider.
  - `files`: A `List<FileData>` representing the files to be included in the command.
  - `isShellMode`: A `Boolean` that, if true, enables shell mode for Aider.
  - `lintCmd`: A `String` specifying the command to run for linting the code.
  - `deactivateRepoMap`: A `Boolean` that, if true, disables the repository mapping feature.
  - `editFormat`: A `String` specifying the format for edit instructions (e.g., "diff").
  - `projectPath`: A `String` representing the path to the project.
  - `useDockerAider`: An optional `Boolean` indicating whether to use Docker for Aider.
  - `structuredMode`: A `Boolean` indicating if structured mode is enabled.

## Integration and Dependencies

- **Integration Points**: This module interacts with other parts of the Coding Aider system by providing structured data for command execution.
- **Dependencies**: Relies on Kotlin's standard library for data class functionality.

## Design Patterns

- Utilizes the data class pattern to encapsulate and manage data efficiently.

This documentation provides a high-level overview of the module's purpose, functionality, and integration within the larger system. It is intended to assist developers and maintainers in understanding the module's role and usage.
