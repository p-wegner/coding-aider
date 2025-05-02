# [Coding Aider Plan]

## Overview
This plan outlines the implementation of a cost tracking system for plan executions in the Coding Aider plugin. The system will track and display the cost of each plan execution, create execution history files, and provide a service to handle token/cost/model information parsing and storage.

## Problem Description
Currently, the Coding Aider plugin executes plans but does not track or display the cost associated with these executions. Users have no way to monitor their spending on AI model usage or review the history of plan executions with their associated costs. The example in prompt.txt shows that cost information is available in the output but is not being captured or displayed in a structured way.

## Goals
1. Create a service to parse and store execution cost information (tokens, price, model)
2. Generate and maintain execution history markdown files for each plan
3. Display cost information in the UI after plan execution
4. Provide a way to view historical execution costs for plans
5. Integrate with existing plan execution workflow

## Implementation Details

### Cost Tracking Service
Create a new service (`PlanExecutionCostService`) that will:
- Parse LLM responses to extract token counts, costs, and model information
- Store execution history for each plan
- Create and update history markdown files
- Provide methods to retrieve cost information

### Execution History Files
For each plan, create a corresponding history file (e.g., `plan_name_history.md`) that will:
- Track each execution with timestamp
- Record the model used
- Store token counts (sent/received)
- Calculate and display costs
- Include a summary of changes made

### UI Integration
- Update the plan viewer to show the latest execution cost
- Add a way to view execution history for a plan
- Display total cost information in the plans panel

### Integration Points
- Hook into the command execution process to capture outputs
- Parse the command output for cost information
- Update history files after successful plan execution

## Additional Notes and Constraints
- Cost information should be extracted from the LLM output format shown in prompt.txt
- History files should be stored alongside plan files in the `.coding-aider-plans` directory
- The implementation should handle cases where cost information is not available
- The service should be resilient to changes in the output format
- Consider adding configuration options for cost tracking (enable/disable)

## References
- Example output format in prompt.txt: `Tokens: 19k sent, 965 received. Cost: $0.0061 message, $0.0061 session.`
- Existing plan execution flow in `CommandExecutor.kt` and `IDEBasedExecutor.kt`
- Plan storage in `.coding-aider-plans` directory
