# [Coding Aider Plan - Checklist]

## Implementation Tasks

### Service Creation
- [ ] Create TodoExtractionService as a project-scoped service
- [ ] Move TODO extraction logic from BaseFixTodoAction to the service
- [ ] Add method to extract TODOs from a single PsiFile
- [ ] Add method to extract TODOs from multiple files
- [ ] Add method to format TODOs consistently with existing format
- [ ] Add proper error handling and null safety

### Service Integration
- [ ] Update BaseFixTodoAction to use the new TodoExtractionService
- [ ] Update FixTodoAction to use the service
- [ ] Update FixTodoInteractive to use the service
- [ ] Ensure all existing functionality continues to work

### UI Button Addition
- [ ] Add new ActionButton to AiderInputDialog for TODO collection
- [ ] Create appropriate icon and tooltip for the button
- [ ] Position button near other action buttons (copy context, restore, settings)
- [ ] Implement button action to collect TODOs from context files

### TODO Collection Logic
- [ ] Implement method to get all context files from AiderContextView
- [ ] Filter context files to only include readable/existing files
- [ ] Extract TODOs from all context files using the service
- [ ] Format collected TODOs with file attribution
- [ ] Append formatted TODOs to existing input field content

### Error Handling and Edge Cases
- [ ] Handle case when no context files are selected
- [ ] Handle case when no TODOs are found in any files
- [ ] Handle PSI access errors gracefully
- [ ] Provide user feedback for successful/failed operations
- [ ] Handle large numbers of TODOs appropriately

### Testing and Validation
- [ ] Test with single file containing TODOs
- [ ] Test with multiple files containing TODOs
- [ ] Test with files containing no TODOs
- [ ] Test with empty context
- [ ] Test that existing TODO actions still work
- [ ] Verify proper formatting and file attribution
