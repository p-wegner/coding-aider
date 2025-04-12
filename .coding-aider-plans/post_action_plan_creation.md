[Coding Aider Plan]

## Overview
Add functionality to create plans from completed Aider actions, allowing users to convert regular actions into structured plans when they turn out to be more complex than initially anticipated. This will be implemented as an optional, user-initiated action through UI buttons.

## Problem Description
Currently, when users complete an Aider action, there's no built-in way to convert that action and its results into a structured plan. This makes it difficult to properly track and manage tasks that turn out to be more complex than initially expected.

## Goals
1. Enable user-initiated plan creation from completed Aider actions
2. Provide UI buttons in both the output dialog and running commands toolwindow
3. Reuse existing structured mode functionality for plan creation
    - No need to manually create plan files
    - aider will automatically create plan files based on the prompt
4. The prompt to create a plan should include the original command output and context

## Implementation Approach
1. UI Components:
   - Add "Create Plan" button to command output dialogs
   - Add "Convert to Plan" action in running commands toolwindow
   - Use existing plan creation dialog with pre-filled content

2. Data Flow:
   - Capture command output and context from RunningCommandService
   - Create suitable CommandData object for plan creation
   - Run Plan Creation Command from a service

## Additional Notes and Constraints
- Plans should be created in the .coding-aider-plans directory
- Use existing structured mode infrastructure
- Preserve original command output and context
- Plans should include both what was done and any identified follow-up work
- The feature must be explicitly triggered by the user, not automatic
- Provide clear UI affordances for creating plans from completed actions
- Maintain backward compatibility with existing plans
