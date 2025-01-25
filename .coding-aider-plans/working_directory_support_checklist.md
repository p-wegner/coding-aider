# [Coding Aider Plan - Checklist]

## Implementation Steps

### Project Settings
- [x] Add workingDirectory field to AiderProjectSettings
- [x] Update settings state class to include working directory
- [x] Add working directory persistence logic

### Tool Window UI
- [x] Create new WorkingDirectoryPanel component
- [x] Add directory selection button and path display
- [x] Add clear/reset button
- [x] Integrate panel into CodingAiderToolWindowContent
- [x] Add validation for selected directory

### Command Execution
- [ ] Update CommandExecutor to use working directory setting
- [ ] Add subtree-only flag when working directory is set
- [ ] Update process builder directory configuration
- [ ] Handle path normalization and validation

### Documentation
- [ ] Update README with working directory feature
