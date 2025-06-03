# [Coding Aider Plan - Checklist]

## Custom Actions Feature Implementation

### Core Components
- [x] Create CustomActionConfiguration data class with path handling methods
- [x] Create DefaultCustomActions with predefined action templates
- [x] Create CustomActionPromptService for building AI prompts
- [x] Create CustomActionDialog for executing custom actions
- [x] Create CustomActionConfigDialog for managing action configurations

### Settings Integration
- [x] Extend AiderProjectSettings.State to include customActions field
- [x] Add custom action management methods to AiderProjectSettings
- [x] Extend AiderProjectSettingsConfigurable to include custom actions panel
- [x] Add CustomActionRenderer for displaying actions in settings

### UI Integration
- [ ] Add menu action to trigger custom actions from file selection
- [ ] Add toolbar button for quick access to custom actions
- [ ] Add keyboard shortcut for custom actions
- [ ] Test custom action dialog with various file selections

### Documentation
- [ ] Update features.md to document custom actions
- [ ] Add usage examples and screenshots
- [ ] Document configuration options and best practices

### Testing
- [ ] Test custom action creation and editing
- [ ] Test custom action execution with context files
- [ ] Test path handling (relative/absolute conversion)
- [ ] Test settings persistence and loading
- [ ] Test error handling and validation

### Polish
- [ ] Add icons for custom actions
- [ ] Improve dialog layouts and user experience
- [ ] Add tooltips and help text
- [ ] Implement drag-and-drop for context files
