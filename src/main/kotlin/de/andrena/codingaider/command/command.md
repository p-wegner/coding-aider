# Command Module Documentation

## Overview

The Command module is responsible for defining the data structures used to execute commands within the Aider system. It encapsulates the necessary information for command execution, including the command message, file data, and various execution flags.

## Key Classes

### FileData

The `FileData` class represents a file's metadata, including its path and whether it is read-only.

#### Properties:
- `filePath: String` - The path to the file.
- `isReadOnly: Boolean` - Indicates if the file is read-only.

### CommandData

The `CommandData` class encapsulates all the data required to execute an Aider command.

#### Properties:
- `message: String` - The main message or instruction for Aider.
- `useYesFlag: Boolean` - If true, automatically confirms all prompts.
- `llm: String` - The language model to be used (e.g., "gpt-4").
- `additionalArgs: String` - Any additional command-line arguments for Aider.
- `files: List<FileData>` - List of files to be included in the Aider command.
- `isShellMode: Boolean` - If true, enables shell mode for Aider.
- `lintCmd: String` - Command to run for linting the code.
- `deactivateRepoMap: Boolean` - If true, disables the repository mapping feature.
- `editFormat: String` - Specifies the format for edit instructions (e.g., "diff").
- `projectPath: String` - The path to the project.
- `useDockerAider: Boolean?` - Optional flag to use Docker Aider.
- `structuredMode: Boolean` - If true, enables structured mode.

## Design Patterns

The Command module follows the Data Transfer Object (DTO) pattern, which is used to transfer data between software application subsystems.

## Dependencies

The Command module interacts with other modules in the Aider system, particularly those responsible for executing commands and managing file operations.

