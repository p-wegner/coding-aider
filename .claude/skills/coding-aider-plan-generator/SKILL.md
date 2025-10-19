---
name: coding-aider-plan-generator
description: Generate coding-aider plans as created by the IntelliJ coding-aider plugin. Use when user explicitly asks for a "coding-aider plan", "aider plan", or needs structured development planning with checklist tracking and file context management.
allowed-tools: Read, Write, Edit, Grep, Glob, Bash
---

# Coding-Aider Plan Generator

Generate structured coding-aider plans that mirror the IntelliJ coding-aider plugin's plan system, complete with overview, goals, implementation checklist, and file context management.

## Overview

This skill creates coding-aider plans in the exact format used by the IntelliJ coding-aider plugin. Plans include structured overviews, problem descriptions, goals, implementation checklists, and file context management for systematic development tracking.

## When This Skill Activates

Use this skill when the user explicitly requests:
- "Create a coding-aider plan"
- "Generate an aider plan"
- "I need a coding-aider plan for..."
- "Create a structured development plan like coding-aider"
- Similar explicit requests for coding-aider style planning

## Instructions

### 1. Plan Structure Analysis
First, understand the user's requirements:
- What feature or task needs to be implemented?
- What is the current state or problem?
- What are the specific goals and constraints?
- Which files will likely be involved?

### 2. Plan Directory Setup
Create the plan storage structure:
- Create `.coding-aider-plans/` directory if it doesn't exist
- Generate a plan name based on the feature/task (kebab-case)
- Plan files will be stored as: `{plan_name}.md`, `{plan_name}_checklist.md`, `{plan_name}_context.yaml`

### 3. Main Plan File Generation
Create the main plan file (`{plan_name}.md`) with this structure:

```markdown
# [Coding Aider Plan]

# Plan Title

## Overview
High-level description of the feature to be implemented

## Problem Description
Current issues, challenges, or requirements that necessitate this plan

## Goals
1. Specific, measurable goal 1
2. Specific, measurable goal 2
3. Specific, measurable goal 3

## Additional Notes and Constraints
- Technical constraints or limitations
- Dependencies on other systems or features
- Performance considerations
- Security requirements
- Testing requirements

## References
- Links to relevant documentation
- Related files or components
- External resources or examples
```

### 4. Checklist File Generation
Create the checklist file (`{plan_name}_checklist.md`) with atomic implementation tasks:

```markdown
# [Coding Aider Plan - Checklist]

# Plan Title - Implementation Checklist

- [ ] Analysis and research task
- [ ] Setup/initialization task
- [ ] Core implementation task 1
- [ ] Core implementation task 2
- [ ] Integration task
- [ ] Testing task 1
- [ ] Testing task 2
- [ ] Documentation update
- [ ] Code review/refinement
- [ ] Deployment/finalization
```

### 5. Context File Generation
Create the context file (`{plan_name}_context.yaml`) with relevant implementation files:

```yaml
---
files:
  - path: "src/main/kotlin/package/FeatureFile.kt"
    readOnly: false
  - path: "src/test/kotlin/package/FeatureTest.kt"
    readOnly: false
  - path: "build.gradle.kts"
    readOnly: true
```

### 6. Content Generation Guidelines

#### Plan Titles
- Use clear, descriptive titles
- Include the main feature or component name
- Keep titles concise but informative

#### Goals Section
- Make goals specific and measurable
- Focus on outcomes, not implementation details
- Include 3-5 primary goals maximum
- Each goal should be achievable independently

#### Checklist Items
- Break down implementation into atomic tasks
- Each item should represent a single, completable action
- Order tasks logically (setup → implementation → testing → documentation)
- Include both development and validation tasks
- Aim for 8-15 checklist items depending on complexity

#### Context Files
- Include all files that will need modification
- Mark read-only files appropriately (config files, tests to read, etc.)
- Include build files, configuration, and main implementation files
- Group related files by directory/package structure

### 7. File Context Discovery
To identify relevant files for the context.yaml:
- Use Glob to find files matching the feature area
- Use Grep to search for related functionality
- Include configuration files (build.gradle, pom.xml, etc.)
- Include test files for the feature area
- Include documentation files if relevant

### 8. Plan Validation
Before completing, verify:
- Plan name is kebab-case and descriptive
- All three files are created with correct naming
- Checklist items are atomic and actionable
- Context files exist and are correctly marked
- Plan structure matches plugin format exactly

## Examples

### Example 1: New Feature Plan
User request: "Create a coding-aider plan for implementing user authentication"

Generated plan structure:
- `user-authentication.md` - Main plan with auth overview, goals, security constraints
- `user-authentication_checklist.md` - Tasks for JWT setup, password hashing, API endpoints
- `user-authentication_context.yaml` - AuthController, UserService, security config files

### Example 2: Refactoring Plan
User request: "I need a coding-aider plan for refactoring the payment processing module"

Generated plan structure:
- `payment-processing-refactor.md` - Current issues, refactoring goals, migration strategy
- `payment-processing-refactor_checklist.md` - Step-by-step refactoring tasks with testing
- `payment-processing-refactor_context.yaml` - PaymentService, PaymentController, test files

### Example 3: Bug Fix Plan
User request: "Create a coding-aider plan for fixing the memory leak in the data processor"

Generated plan structure:
- `memory-leak-fix.md` - Problem description, root cause analysis, fix approach
- `memory-leak-fix_checklist.md` - Debugging, patching, verification tasks
- `memory-leak-fix_context.yaml` - DataProcessor, related memory management files

## Requirements

- Current working directory should be a project directory
- Sufficient permissions to create `.coding-aider-plans/` directory
- Access to project files for context analysis

## Troubleshooting

### Plan Directory Issues
- If `.coding-aider-plans/` cannot be created, check directory permissions
- Ensure working in a valid project directory with write access

### File Context Discovery
- If no relevant files are found, ask user to specify key files
- Use broader search patterns with Glob if specific searches fail
- Include common project files (build configs, main source directories)

### Plan Naming
- Use kebab-case for plan names (convert spaces to hyphens)
- Keep names under 50 characters for readability
- Avoid special characters beyond hyphens

### Checklist Quality
- If checklist items are too broad, break them into smaller atomic tasks
- Ensure each item can be completed independently
- Include both development and validation/testing tasks