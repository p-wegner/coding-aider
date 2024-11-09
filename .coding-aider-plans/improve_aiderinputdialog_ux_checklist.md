[Coding Aider Plan - Checklist]
# Improve AiderInputDialog UX Implementation Checklist

Related: [Plan](improve_aiderinputdialog_ux.md)

## Setup
- [x] Create new UI components for collapsible panel using IntelliJ UI.Panels
- [x] Add collapsed state property to AiderProjectSettings

## UI Changes
- [x] Move LLM selection components to bottom row
- [x] Create collapsible panel for options row
- [x] Add toggle button with icon for expand/collapse
- [x] Update layout constraints for new organization
- [x] Implement smooth animation for panel transitions

## State Management
- [x] Add collapsed state persistence to settings
- [x] Implement state restoration on dialog creation
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
