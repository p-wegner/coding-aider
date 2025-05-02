# [Coding Aider Plan]

# Documentation Types Configuration

## Overview
This plan outlines the implementation of a configurable documentation type system for the CodingAider plugin. Similar to the existing test generation feature, users will be able to select from different documentation types (technical, PRD, etc.) when generating documentation for their code.

## Problem Description
Currently, the documentation actions in CodingAider (`DocumentCodeAction` and `DocumentEachFolderAction`) use hardcoded prompts for generating documentation. This limits flexibility and doesn't allow users to customize the type of documentation they want to generate based on their specific needs.

The test generation feature already has a configurable system that allows users to define different test types with custom prompts and patterns. We need to implement a similar system for documentation generation.

## Goals
1. Create a `DocumentTypeConfiguration` data class similar to `TestTypeConfiguration`
2. Implement a `DocumentationGenerationPromptService` similar to `TestGenerationPromptService`
3. Add document type settings to `AiderProjectSettings`
4. Create UI components for managing document types in settings
5. Update `DocumentCodeAction` and `DocumentEachFolderAction` to use the new configurable system
6. Provide default document types (Technical, PRD, User Guide, etc.)

## Additional Notes and Constraints
- Maintain backward compatibility with existing documentation features
- Reuse as much of the existing test type configuration UI as possible
- Ensure the documentation types are stored in project settings
- Allow users to customize prompts for each documentation type
- Support context files for documentation types similar to test types

## References
- Existing test type configuration: `TestTypeConfiguration.kt`
- Test generation prompt service: `TestGenerationPromptService.kt`
- Documentation actions: `DocumentCodeAction.kt` and `DocumentEachFolderAction.kt`
- Project settings: `AiderProjectSettings.kt`
