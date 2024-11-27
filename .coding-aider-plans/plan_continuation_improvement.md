[Coding Aider Plan]

## Overview
Improve the plan continuation functionality to ensure consistent behavior across different continuation methods and proper tracking of the current plan being executed.

## Problem Description
Currently, there are multiple ways to continue a plan:
1. Via the "Close & Continue" button in the MarkdownDialog
2. Through the autoclose feature in MarkdownDialog
3. Via AiderAction with structuredMode
4. Directly through ContinuePlanService

This creates potential inconsistencies in which plan is being continued. The system needs to reliably track and continue the correct plan regardless of the continuation method used.

## Goals
1. Create a centralized service to track the current active plan
2. Ensure consistent plan continuation behavior across all methods
3. Properly decouple plan-related functionality
4. Improve reliability of plan continuation

## Additional Notes and Constraints
- Must maintain backward compatibility with existing plan functionality
- Should handle edge cases like multiple plans being executed simultaneously
- Need to ensure proper cleanup when plans are completed

## References
[Checklist](plan_continuation_improvement_checklist.md)
