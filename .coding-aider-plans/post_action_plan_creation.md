[Coding Aider Plan]

## Overview
Add functionality to create plans from completed Aider actions, allowing users to convert regular actions into structured plans when they turn out to be more complex than initially anticipated.

## Problem Description
Currently, when users complete an Aider action, there's no built-in way to convert that action and its results into a structured plan. This makes it difficult to properly track and manage tasks that turn out to be more complex than initially expected.

## Goals
1. Enable automatic plan creation from completed Aider actions
2. Reuse existing structured mode functionality for plan creation
3. Preserve action context and results in the generated plan
4. Make the feature configurable per action

## Additional Notes and Constraints
- Plans should be created in the .coding-aider-plans directory
- Use existing structured mode infrastructure
- Preserve original command output and context
- Consider adding the feature as an option in CommandOptions
- Plans should include both what was done and any identified follow-up work

## References
- CommandData.kt for CommandOptions structure
- AiderPlanService.kt for plan creation logic
- CommandExecutor.kt for execution flow
