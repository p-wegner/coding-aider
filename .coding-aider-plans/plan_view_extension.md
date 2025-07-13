[Coding Aider Plan]

# Plan View Extension

## Overview

This plan focuses on enhancing the plan view functionality in the Coding-Aider plugin by unifying plan modification actions into a context menu and adding a new verification action that uses LLM to verify implementation and update checklists automatically.

## Problem Description

Currently, the plan view has several actions scattered in the toolbar (refine plan, edit context file, etc.) which makes the interface cluttered and less intuitive. Additionally, there's no automated way to verify if implementation tasks have been completed and update the checklist accordingly, requiring manual checklist management.

The current issues include:
1. Plan modification actions are spread across the toolbar making the UI cluttered
2. No context menu for right-click operations on plans
3. Manual checklist management without automated verification
4. Missing LLM-powered implementation verification capability

## Goals

1. **✅ Unify Plan Actions**: Move plan modification actions (refine plan, edit context file) from the toolbar into a context menu that appears on right-click
2. **✅ Context Menu Implementation**: Create a comprehensive context menu system for plan items
3. **✅ LLM Verification Action**: Add a new action that prompts the LLM to verify implementation status and automatically update checklist items
4. **✅ Improved UX**: Provide a cleaner, more intuitive interface for plan management
5. **✅ Automated Checklist Updates**: Reduce manual work by having the system automatically mark completed tasks

## Implementation Status

**COMPLETED** - All goals have been successfully implemented:

- Context menu system is fully functional with right-click support
- Plan modification actions (refine, edit context, verify implementation) are accessible via context menu
- LLM verification action automatically analyzes implementation status and updates checklists
- Toolbar has been cleaned up, keeping only essential actions (New Plan, Refresh)
- Keyboard shortcuts are available for all context menu actions
- Comprehensive error handling and user feedback is in place

## Additional Notes and Constraints

- The context menu should be accessible via right-click on plan items in the plan list
- The verification action should analyze the current state of files and compare against checklist items
- The LLM verification should be configurable (model selection, prompts)
- Existing toolbar actions should be preserved for backward compatibility initially
- The verification process should be non-destructive and allow user review before applying changes
- Context menu actions should be context-aware (enabled/disabled based on plan state)

## References

- [plan_view_extension_checklist.md](plan_view_extension_checklist.md)
- [plan_view_extension_context.yaml](plan_view_extension_context.yaml)
