[Coding Aider Plan]

## Overview
Implement a plan refinement feature that allows users to modify and extend existing plans through a dedicated dialog interface. This feature enables iterative plan development by letting users refine implementation details, add new requirements, or create subplans as needed.

## Problem Description
Currently, plans are static once created. There's no built-in way to:
- Modify existing plans to accommodate new requirements
- Break down complex tasks into subplans after initial creation
- Extend plans based on implementation feedback
- Refine implementation details as understanding improves

## Goals
1. Create a user-friendly dialog for plan refinement
2. Enable viewing of existing plan content during refinement
3. Support creation of subplans through refinement
4. Maintain plan hierarchy and relationships
5. Provide clear visual feedback of plan structure
6. Preserve existing plan content while adding refinements

## Additional Notes and Constraints
- Must integrate with existing plan viewer UI
- Should support both light and dark themes
- Dialog should show current plan content for reference
- Refinement requests should be processed by the LLM
- Must maintain proper plan file structure and naming conventions
- Should handle both simple refinements and complex subplan creation
