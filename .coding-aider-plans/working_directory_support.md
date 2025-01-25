# [Coding Aider Plan]

## Overview
Add support for configuring a working directory for Aider commands and enable the subtree-only option. This will allow users to restrict Aider operations to specific project subdirectories and improve context management.

## Problem Description
Currently, Aider always operates from the project root directory, which can lead to:
- Unnecessary file scanning in large repositories
- Aider-related files being scattered across the project
- Lack of focused context when working on specific modules

## Goals
1. Add a new "Working Directory" section to the Coding Aider tool window
2. Store working directory configuration in project settings
3. Apply working directory setting to all Aider commands
4. Enable subtree-only mode when working directory is set
5. Persist settings across IDE restarts

## Additional Notes and Constraints
- Working directory must be within the project root
- Setting should be optional (null = use project root)
- Must handle path normalization for cross-platform compatibility
- Should validate directory exists and is within project
- Need to update command execution to respect working directory
- Consider UX for directory selection (file chooser dialog)

## References
- [Aider subtree-only documentation](https://aider.chat/docs/config/options.html#subtree-only)
- [IntelliJ Platform SDK - Settings](https://plugins.jetbrains.com/docs/intellij/settings.html)
- [IntelliJ Platform SDK - File Chooser](https://plugins.jetbrains.com/docs/intellij/file-chooser.html)
