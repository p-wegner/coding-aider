[Coding Aider Plan - Checklist]
# Improve AiderInputDialog UX Implementation Checklist

Related: [Plan](improve_aiderinputdialog_ux.md)

## Setup
- [x] Create new UI components for collapsible panel using IntelliJ UI.Panels
- [x] Add collapsed state property to AiderProjectSettings

## UI Changes
- [ ] Move LLM selection components to bottom row
- [ ] Create collapsible panel for options row
- [ ] Add toggle button with icon for expand/collapse
- [ ] Update layout constraints for new organization
- [ ] Implement smooth animation for panel transitions

## State Management
- [ ] Add collapsed state persistence to settings
- [ ] Implement state restoration on dialog creation
- [ ] Handle state changes during runtime

## Testing & Refinement
- [ ] Test resize behavior
- [ ] Verify keyboard shortcuts still work
- [ ] Check proper state persistence
- [ ] Validate animation performance
- [ ] Test with different themes and look-and-feels

## Documentation
- [ ] Update comments in AiderInputDialog
- [ ] Document new settings property
- [ ] Add tooltips for collapse/expand functionality
