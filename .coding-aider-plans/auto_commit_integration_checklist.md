# [Coding Aider Plan - Checklist]

## UI Changes
- [x] Reorganize settings UI to group plugin-based edits, lenient edits, and prompt augmentation together
- [x] Add a new "Auto Commit" checkbox setting in the appropriate section
- [x] Update tooltips to clarify the relationship between features

## Backend Implementation
- [x] Add auto commit setting to AiderSettings.kt
- [x] Create a service to extract commit messages from LLM responses
- [x] Implement commit functionality that triggers after successful plugin-based edits
- [x] Integrate with existing Git settings (dirty commits, etc.)

## Testing and Validation
- [ ] Test the auto commit feature with various LLM responses
- [ ] Verify proper error handling when commit message can't be extracted
- [x] Ensure settings are properly saved and loaded
