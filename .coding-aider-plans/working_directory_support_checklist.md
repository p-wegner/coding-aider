# [Coding Aider Plan - Checklist]

## Implementation Steps

### Project Settings
- [ ] Add workingDirectory field to AiderProjectSettings
- [ ] Update settings state class to include working directory
- [ ] Add working directory persistence logic

### Tool Window UI
- [ ] Create new WorkingDirectoryPanel component
- [ ] Add directory selection button and path display
- [ ] Add clear/reset button
- [ ] Integrate panel into CodingAiderToolWindowContent
- [ ] Add validation for selected directory

### Command Execution
- [ ] Update CommandExecutor to use working directory setting
- [ ] Add subtree-only flag when working directory is set
- [ ] Update process builder directory configuration
- [ ] Handle path normalization and validation

### Testing
- [ ] Test working directory persistence
- [ ] Test directory selection UI
- [ ] Test command execution with working directory
- [ ] Test subtree-only flag application
- [ ] Test path validation and normalization

### Documentation
- [ ] Update README with working directory feature
- [ ] Add tooltips and UI help text
- [ ] Document configuration options
