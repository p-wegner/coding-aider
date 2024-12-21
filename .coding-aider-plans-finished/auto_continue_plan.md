[Coding Aider Plan]

# Automatic Plan Continuation Feature

## Overview
Add functionality to automatically continue executing a plan when there are open checklist items, triggered either by dialog autoclose or a new "Close and Continue" button.

## Problem Description
Currently, when working with plans in structured mode, users need to manually trigger continuation of plan execution after each step. This creates unnecessary friction in the workflow, especially when there are multiple small tasks to complete.

## Goals
1. Add a new "Close and Continue" button to the MarkdownDialog
2. Implement automatic plan continuation on dialog autoclose
3. Only trigger continuation when there are open checklist items
4. Maintain existing abort/close functionality
5. Ensure proper cleanup of resources

## Additional Notes and Constraints
- Must respect existing autoclose settings
- Should only apply in structured mode
- Must handle edge cases (e.g., errors during continuation)
- Should maintain existing dialog behavior for non-plan executions
- Must properly clean up resources and handle window disposal

## References
[Checklist](auto_continue_plan_checklist.md)
