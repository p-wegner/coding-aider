# [Coding Aider Plan - Checklist]

## Custom Actions Implementation Checklist

### Core Implementation
- [x] Create `CustomActionConfiguration` data class with path conversion methods
- [x] Create `DefaultCustomActions` with predefined actions (Code Review, Documentation, Refactor)
- [x] Create `CustomActionPromptService` for building prompts
- [x] Create `CustomActionDialog` for executing actions
- [x] Create `CustomActionTypeDialog` for managing action configurations

### Settings Integration
- [x] Extend `AiderProjectSettings.State` to include custom actions
- [x] Add custom action management methods to `AiderProjectSettings`
- [x] Extend `AiderProjectSettingsConfigurable` with custom actions panel
- [x] Add custom action renderer for the settings UI

### UI Integration
- [ ] Add menu action to trigger custom actions dialog
- [ ] Add context menu integration for file/folder selection
- [ ] Add keyboard shortcuts for common actions
- [ ] Add toolbar buttons (optional)

### Documentation
- [ ] Update main documentation with custom actions feature
- [ ] Add usage examples
- [ ] Document configuration options

### Testing & Polish
- [ ] Test custom action creation and execution
- [ ] Test context file management
- [ ] Test settings persistence
- [ ] Verify error handling
- [ ] Test with different file types and project structures

### Future Enhancements
- [ ] Action categories/groups
- [ ] Import/export action configurations
- [ ] Action templates marketplace
- [ ] Conditional actions based on file types
