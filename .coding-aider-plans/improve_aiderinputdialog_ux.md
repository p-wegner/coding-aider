[Coding Aider Plan]
# Improve AiderInputDialog UX

## Overview
Improve the user experience of AiderInputDialog by reorganizing the layout to hide less frequently used options and create a cleaner interface.

## Current Issues
- LLM selection, yes flag, and additional arguments take up valuable screen space
- These options are not frequently changed during normal usage
- Interface feels cluttered with too many controls visible at once

## Proposed Changes
1. Move LLM selection to the bottom row with yes flag and additional arguments
2. Add a collapsible panel for these less frequently used options
3. Add a toggle button to show/hide the options panel
4. Save the panel's collapsed state in settings

## Implementation Details
- Create a new collapsible panel component for the options
- Move LLM selection UI elements to the bottom row
- Add animation for smooth collapse/expand transitions
- Persist the collapsed state across sessions
- Update layout constraints to accommodate the new organization

## Benefits
- Cleaner, more focused interface for common operations
- Advanced options remain easily accessible when needed
- More vertical space for the main input area
- Better organization of related controls

## Related Files
- [Checklist](improve_aiderinputdialog_ux_checklist.md)

## Technical Considerations
- Use existing IntelliJ UI components for consistency
- Maintain keyboard shortcuts and mnemonics
- Ensure proper layout behavior when resizing
- Handle state persistence through existing settings framework
