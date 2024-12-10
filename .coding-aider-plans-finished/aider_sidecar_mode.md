[Coding Aider Plan]

# Aider Sidecar Mode Implementation

## Overview

Add a new execution mode that runs Aider as a long-lived sidecar process, maintaining an active session throughout the
plugin's lifecycle rather than starting a new process for each command.

## Problem Description

Currently, the plugin starts a new Aider process for each command execution, which is inefficient and doesn't take
advantage of Aider's interactive capabilities. This approach:

- Creates overhead from process startup/shutdown
- Doesn't maintain conversation context between commands
- Limits potential for interactive features
- Makes it harder to implement real-time feedback

## Goals

1. Implement a sidecar execution strategy that:
    - Starts Aider on plugin initialization
    - Maintains persistent communication via stdin/stdout
    - Parses terminal output for state management
    - Preserves existing execution modes as alternatives

2. Abstract the interaction pattern to:
    - Support both current and sidecar modes
    - Enable future reactive/interactive features
    - Maintain backward compatibility
    - Work with both native and Docker execution

## Parsing Aider Output

On Startup: aider is ready as soon as an empty line is received on the output stream
On Command Execution: aider will finish its command with a "> " line on the output stream

## Additional Notes and Constraints

- Must preserve existing CommandExecutor functionality
- Need robust output parsing due to Aider's terminal-based nature
- Should handle process lifecycle with plugin startup/shutdown
- Must work with both native and Docker execution modes
- Should maintain proper error handling and recovery
- Must clean up resources properly on plugin shutdown

## References

- [Current CommandExecutor](./src/main/kotlin/de/andrena/codingaider/executors/CommandExecutor.kt)
- [AiderExecutionStrategy](./src/main/kotlin/de/andrena/codingaider/executors/AiderExecutionStrategy.kt)
- [Aider documentation](https://aider.chat/docs/)

See [aider_sidecar_mode_checklist.md](aider_sidecar_mode_checklist.md) for implementation steps.
