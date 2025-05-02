[Coding Aider Plan]

## Overview
This plan introduces a new boolean setting, `pluginBasedEdits`, to control how code modification commands are sent to Aider. When enabled, commands will be prefixed with `/ask`, bypassing Aider's internal edit format handling and instead relying on the plugin to parse and apply changes based on LLM output formatted according to specific instructions.

## Problem Description
Aider's built-in edit formats (like `diff`, `whole`, `udiff`) are powerful but might conflict with custom parsing logic within the plugin or specific user workflows. Using Aider's `/ask` command allows for more direct interaction with the LLM, but it requires explicitly instructing the LLM on how to format its response, particularly for code changes, so the plugin can process it correctly.

## Goals
1.  **Add Setting:** Introduce a new boolean setting `pluginBasedEdits` in `AiderSettings.kt` (defaulting to `false`).
2.  **Add UI Control:** Add a corresponding checkbox in the `AiderSettingsConfigurable.kt` UI to allow users to enable/disable this mode.
3.  **Modify Command Execution:** Update the command building logic (likely in `AiderExecutionStrategy.kt` and potentially `CommandExecutor.kt` for sidecar mode) to:
    *   Check the state of the `pluginBasedEdits` setting.
    *   If enabled, prepend `/ask ` to the user's message before sending it to Aider.
    *   If enabled, prepend specific instructions to the user's message (after `/ask `) directing the LLM to format code changes using the `diff` format as defined in `ClipboardEditService.kt`.

## Additional Notes and Constraints
*   The plugin's `ClipboardEditService` already supports parsing the `diff` format.
*   The prompt instructions for the LLM must be clear and accurately describe the required `diff` format (`<<<<<<< SEARCH`, `=======`, `>>>>>>> REPLACE`).
*   Ensure the change works correctly for standard execution, Docker execution, and sidecar mode.
