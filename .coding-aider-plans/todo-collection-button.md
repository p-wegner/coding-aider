# [Coding Aider Plan]

## Overview

Add a button to the Aider input dialog that collects all TODO comments from the current context files and appends them to the prompt input field. This feature will help users quickly gather all outstanding TODO items from their selected files and address them in a single Aider session.

## Problem Description

Currently, users need to manually identify and copy TODO comments from their files when they want to address multiple TODOs at once. The existing FixTodoAction only works on individual files and requires separate invocations for each file. There's no convenient way to collect all TODOs from multiple context files in the Aider input dialog.

The current TODO-related functionality is scattered across the FixTodoAction classes, making it difficult to reuse the TODO extraction logic in other parts of the application.

## Goals

1. **Extract TODO functionality**: Create a dedicated service to handle TODO extraction logic that can be reused across the application
2. **Add collection button**: Add a new button to the Aider input dialog that triggers TODO collection from all context files
3. **Append to prompt**: Collected TODOs should be appended to the existing content in the input field, not replace it
4. **Consistent formatting**: Use the same TODO formatting as the existing FixTodoAction classes
5. **User experience**: Provide visual feedback and handle edge cases (no TODOs found, empty context, etc.)

## Additional Notes and Constraints

- The TODO extraction logic should be moved to a service to avoid code duplication
- The button should be placed near other action buttons in the input dialog
- TODOs should be formatted consistently with the existing `fixTodoPrompt` format
- The feature should handle multiple files gracefully and provide clear file attribution for each TODO
- Consider performance implications when scanning large numbers of files
- The service should be project-scoped to allow proper PSI access

## References

- [todo-collection-button_checklist.md](todo-collection-button_checklist.md) - Implementation checklist
- [todo-collection-button_context.yaml](todo-collection-button_context.yaml) - Affected files context
