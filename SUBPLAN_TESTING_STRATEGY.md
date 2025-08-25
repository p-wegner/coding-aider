# Comprehensive Subplan Execution Testing Strategy

## Problem Analysis
We need deterministic tests for subplan execution flow without relying on LLMs. The key challenges are:
1. **File Context Selection**: Testing which files are included based on subplan state
2. **Plan State Transitions**: Testing subplan completion and next subplan selection  
3. **Prompt Generation**: Testing subplan-specific vs root plan prompts
4. **Deterministic Plan Creation**: Using predefined markdown plans instead of LLM generation

## Testing Strategy

### 1. Create Test Plan Resources
- **Root Plan with Subplans**: Multi-file format with main plan, checklist, and context files
- **Single-File Plan**: All-in-one markdown with embedded checklist and context
- **Subplan Files**: Individual subplan markdown files referenced by root plan
- **Context YAML Files**: Implementation files for each subplan

### 2. Core Test Classes

#### New Test Files to Create:
- `src/test/kotlin/de/andrena/codingaider/services/plans/SubplanExecutionTest.kt`
- `src/test/kotlin/de/andrena/codingaider/services/plans/ActivePlanServiceTest.kt`
- `src/test/kotlin/de/andrena/codingaider/services/plans/AiderPlanServiceSubplanTest.kt`
- `src/test/kotlin/de/andrena/codingaider/services/plans/SubplanPromptServiceTest.kt`
- `src/test/kotlin/de/andrena/codingaider/utils/TestPlanFactory.kt`

#### Test Class Purposes:
- **`SubplanExecutionTest`**: Integration tests for full subplan execution flow
- **`ActivePlanServiceTest`**: Unit tests for subplan state management
- **`AiderPlanServiceSubplanTest`**: Unit tests for plan loading and parsing with subplans
- **`SubplanPromptServiceTest`**: Unit tests for subplan-aware prompt generation
- **`TestPlanFactory`**: Factory for creating deterministic test plan structures

### 3. Key Test Scenarios

#### A. File Context Selection Tests
- Root plan only (no subplans) → includes all plan files
- First subplan execution → includes root plan files + first subplan files  
- Second subplan execution → includes root plan files + second subplan files
- Root plan after all subplans complete → includes all plan files

#### B. State Transition Tests
- Plan continuation when first subplan incomplete → executes first subplan
- Subplan completion → transitions to next subplan
- All subplans complete → transitions to root plan
- Plan family completion → clears active plan

#### C. Prompt Generation Tests
- Root plan execution → standard plan prompt
- Subplan execution → subplan context prompt with progress info
- Subplan completion → next subplan prompt

### 4. Test Infrastructure

#### A. Test Plan Factory
- `TestPlanFactory` for creating deterministic plan structures
- Predefined plan templates in test resources
- Helper methods for creating plans with various completion states

#### B. Mock Services
- Mock `AiderPlanService` for controlled plan loading
- Mock `FileDataCollectionService` for file selection simulation
- Test doubles for filesystem operations

#### C. Test Resources Structure
Create these test resource files:

```
src/test/resources/plans/
├── multi_file_plans/
│   ├── root_plan_with_subplans.md
│   ├── root_plan_checklist.md  
│   ├── root_plan_context.yaml
│   ├── auth_subplan.md
│   ├── auth_subplan_checklist.md
│   ├── auth_subplan_context.yaml
│   ├── ui_subplan.md
│   ├── ui_subplan_checklist.md
│   └── ui_subplan_context.yaml
├── single_file_plans/
│   ├── complete_single_file_plan.md
│   ├── partial_single_file_plan.md
│   └── single_file_with_subplans.md
└── context_files/
    ├── auth_impl_files.yaml
    ├── ui_impl_files.yaml
    └── shared_impl_files.yaml
```

### 5. Production Code Refactoring (if needed)

#### Files that may need modification for testing:
- `src/main/kotlin/de/andrena/codingaider/services/plans/ActivePlanService.kt`
  - Add test-friendly constructors
  - Extract file collection logic for easier mocking
  - Expose internal state getters for verification

- `src/main/kotlin/de/andrena/codingaider/services/plans/AiderPlanService.kt`
  - Add deterministic plan loading methods
  - Extract file system operations for mocking

- `src/main/kotlin/de/andrena/codingaider/services/plans/AiderPlanPromptService.kt`
  - Make subplan prompt creation more testable
  - Extract prompt building logic

### 6. Test Data Content Examples

#### Root Plan (root_plan_with_subplans.md):
```markdown
# [Coding Aider Plan]

# Authentication and UI System Implementation

## Overview
Implementation of authentication system and user interface components.

## Problem Description
Need to implement secure authentication and modern UI components.

## Goals
1. Implement authentication system with JWT tokens
2. Create responsive UI components
3. Integrate authentication with UI

## Additional Notes and Constraints
- Use existing security frameworks
- Ensure cross-browser compatibility

## References
- Security best practices documentation

<!-- SUBPLAN:auth_subplan -->
[Subplan: Authentication System](auth_subplan.md)
<!-- END_SUBPLAN -->

<!-- SUBPLAN:ui_subplan -->
[Subplan: UI Components](ui_subplan.md)
<!-- END_SUBPLAN -->
```

#### Root Plan Checklist (root_plan_checklist.md):
```markdown
# [Coding Aider Plan - Checklist]

# Authentication and UI System Implementation Checklist

## Main Implementation Tasks

- [ ] Complete authentication system implementation
- [ ] Complete UI components implementation
- [ ] Integrate authentication with UI components
- [ ] Run end-to-end tests
- [ ] Update documentation
```

#### Authentication Subplan (auth_subplan.md):
```markdown
# [Coding Aider Plan]

# Authentication System Implementation

## Overview
Implement JWT-based authentication with login, logout, and token refresh.

## Problem Description
Need secure authentication system with proper token management.

## Goals
1. Implement JWT token generation and validation
2. Create login/logout endpoints
3. Add token refresh mechanism
4. Implement user session management

## Implementation Details
- Use Spring Security framework
- Store tokens securely
- Handle token expiration gracefully
```

### 7. Specific Test Methods to Implement

#### ActivePlanServiceTest.kt methods:
- `shouldExecuteFirstSubplanWhenRootPlanHasIncompleteSubplans()`
- `shouldTransitionToNextSubplanWhenCurrentSubplanCompletes()`
- `shouldReturnToRootPlanWhenAllSubplansComplete()`
- `shouldIncludeCorrectFilesForSubplanExecution()`
- `shouldIncludeCorrectFilesForRootPlanExecution()`
- `shouldHandleSubplanStateRefreshCorrectly()`

#### SubplanExecutionTest.kt methods:
- `shouldExecuteSubplansInSequentialOrder()`
- `shouldIncludeOnlyRelevantFilesForCurrentSubplan()`
- `shouldGenerateSubplanSpecificPrompts()`
- `shouldHandleSubplanCompletionAndTransition()`
- `shouldFallBackToRootPlanWhenAllSubplansComplete()`

#### AiderPlanServiceSubplanTest.kt methods:
- `shouldParseMultiFileSubplansCorrectly()`
- `shouldParseSingleFileSubplansCorrectly()`
- `shouldBuildSubplanHierarchyCorrectly()`
- `shouldHandleSubplanReferencesInMarkdown()`
- `shouldLoadSubplanContextFilesCorrectly()`

#### SubplanPromptServiceTest.kt methods:
- `shouldCreateSubplanExecutionPromptWithContext()`
- `shouldIncludeSubplanProgressInformation()`
- `shouldFallBackToStandardPromptForRootPlan()`
- `shouldHandleCompletedSubplansInPrompt()`

### 8. Expected Test Coverage
- **File Selection**: 95% coverage of `collectVirtualFilesForExecution` logic
- **State Management**: 100% coverage of subplan state transitions
- **Prompt Generation**: 90% coverage of subplan-aware prompt creation
- **Integration Flow**: End-to-end subplan execution simulation

### 9. Implementation Checklist

#### Test Infrastructure:
- [ ] Create test resource directory structure
- [ ] Write test plan markdown files
- [ ] Create context YAML files
- [ ] Implement TestPlanFactory utility

#### Unit Tests:
- [ ] Write ActivePlanServiceTest
- [ ] Write AiderPlanServiceSubplanTest  
- [ ] Write SubplanPromptServiceTest
- [ ] Add subplan tests to existing AiderPlanPromptServiceTest

#### Integration Tests:
- [ ] Write SubplanExecutionTest
- [ ] Add subplan scenarios to existing integration tests

#### Production Code Updates:
- [ ] Add test-friendly methods to ActivePlanService
- [ ] Extract testable logic from AiderPlanService
- [ ] Add logging for test verification
- [ ] Ensure proper error handling for edge cases

This comprehensive testing strategy ensures deterministic verification of subplan execution without relying on LLM responses, while maintaining realistic plan structures through markdown test data.