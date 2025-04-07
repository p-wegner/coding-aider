[Coding Aider Plan]

## Overview
Add functionality to create plans from completed Aider actions, allowing users to convert regular actions into structured plans when they turn out to be more complex than initially anticipated. This will be implemented as an optional, user-initiated action through UI buttons.

## Problem Description
Currently, when users complete an Aider action, there's no built-in way to convert that action and its results into a structured plan. This makes it difficult to properly track and manage tasks that turn out to be more complex than initially expected.

## Goals
1. Enable user-initiated plan creation from completed Aider actions
2. Provide UI buttons in both the output dialog and running commands toolwindow
3. Reuse existing structured mode functionality for plan creation
4. Preserve action context and results in the generated plan

## Additional Notes and Constraints
- Plans should be created in the .coding-aider-plans directory
- Use existing structured mode infrastructure
- Preserve original command output and context
- Plans should include both what was done and any identified follow-up work
- The feature must be explicitly triggered by the user, not automatic
- Provide clear UI affordances for creating plans from completed actions

## Implementation Strategy
The implementation will be divided into three main components:

<!-- SUBPLAN:post_action_plan_creation_ui -->
[Subplan: UI Components for Plan Creation](post_action_plan_creation_ui.md)
<!-- END_SUBPLAN -->


<!-- SUBPLAN:post_action_plan_creation_integration -->
[Subplan: Integration with Existing Systems](post_action_plan_creation_integration.md)
<!-- END_SUBPLAN -->

## References
- CommandData.kt for CommandOptions structure
- AiderPlanService.kt for plan creation logic
- CommandExecutor.kt for execution flow
- MarkdownDialog.kt for output dialog UI modifications
- RunningCommandService.kt for toolwindow integration
