[Coding Aider Plan]

## Overview
Implement the core logic for converting a completed Aider action into a structured plan.

## Problem Description
When a user initiates plan creation from a completed action, the system needs to extract relevant information from the action and transform it into a properly structured plan.

## Goals
1. Extract command context, input, and output from completed actions
2. Generate appropriate plan structure from action data
3. Create all required plan files (main plan, checklist, context)
4. Ensure generated plans follow established conventions

## Implementation Details
### Action Data Extraction
- Extract command input (user prompt)
- Extract command output (AI response)
- Extract file context (files involved in the action)
- Extract metadata (timestamp, LLM used, etc.)

### Plan Generation
- Create plan title and overview based on command input
- Generate problem description from command context
- Create initial goals based on command output
- Identify potential follow-up work from command output
- Generate appropriate checklist items

### File Creation
- Create main plan file with proper structure
- Create checklist file with initial items
- Create context file with relevant file paths

## Additional Notes and Constraints
- Use existing AiderPlanService functionality where possible
- Ensure generated plans follow established naming conventions
- Handle edge cases (empty output, failed commands, etc.)
- Consider performance for large command outputs

## References
- AiderPlanService.kt for plan creation logic
- CommandData.kt for command structure
