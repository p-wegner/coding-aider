[Coding Aider Plan]

# Automatic Test Generation Feature

## Overview
Add a new action to automatically generate tests based on configurable test types. The feature will provide a dialog interface for users to select test types and provide additional prompts, with test type configurations managed through project settings.

## Problem Description
Currently, there is no built-in way to automatically generate tests with AI assistance in the plugin. Manual test creation can be time-consuming and may not follow consistent patterns across a project.

## Goals
1. Create a new action for test generation with a configurable dialog
2. Add test type configuration to project settings
3. Allow flexible test generation with customizable prompts
4. Support context-aware test generation using reference files

## Implementation Details
1. New Dialog Components:
   - Test type selection dropdown
   - Optional multiline prompt input
   - Preview/Settings button

2. Project Settings Extensions:
   - Test type management (add/edit/delete)
   - Per-type configuration:
     - Initial prompt templates
     - Reference file associations
     - Custom parameters

3. Test Generation Logic:
   - Context gathering from selected files
   - Template processing
   - AI prompt construction
   - Test file creation

## Additional Notes and Constraints
- Must integrate seamlessly with existing Aider functionality
- Should support various testing frameworks and styles
- Need to handle both single file and multiple file selections
- Consider test file naming conventions and placement

## References
- [DocumentCodeAction.kt](../src/main/kotlin/de/andrena/codingaider/actions/aider/DocumentCodeAction.kt)
- [AiderProjectSettings.kt](../src/main/kotlin/de/andrena/codingaider/settings/AiderProjectSettings.kt)
- [AiderProjectSettingsConfigurable.kt](../src/main/kotlin/de/andrena/codingaider/settings/AiderProjectSettingsConfigurable.kt)
