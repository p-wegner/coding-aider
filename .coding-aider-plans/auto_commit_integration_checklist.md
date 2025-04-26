# [Coding Aider Plan - Checklist]

## UI Changes
- [ ] Reorganize settings UI to group plugin-based edits, lenient edits, and prompt augmentation together
- [ ] Add a new "Auto Commit" checkbox setting in the appropriate section
- [ ] Update tooltips to clarify the relationship between features

## Backend Implementation
- [ ] Add auto commit setting to AiderSettings.kt
- [ ] Create a service to extract commit messages from LLM responses
- [ ] Implement commit functionality that triggers after successful plugin-based edits
- [ ] Integrate with existing Git settings (dirty commits, etc.)

## Testing and Validation
- [ ] Test the auto commit feature with various LLM responses
- [ ] Verify proper error handling when commit message can't be extracted
- [ ] Ensure settings are properly saved and loaded
