# [Coding Aider Plan - Checklist]

# Migrate to Tool Window Output System - Implementation Checklist

## Settings and Configuration
- [ ] Update AiderDefaults.USE_TOOL_WINDOW_OUTPUT to default to true
- [ ] Remove dialog-specific settings from AiderSettings
- [ ] Remove enableMarkdownDialogAutoclose setting
- [ ] Remove markdownDialogAutocloseDelay setting
- [ ] Remove closeOutputDialogImmediately property
- [ ] Remove showMarkdownDevTools setting (move to general setting if needed)

## AiderOutputService Modifications
- [ ] Remove createDialog method from AiderOutputService
- [ ] Simplify createOutput to only create tool window tabs
- [ ] Remove dialog-specific logic from updateProgress method
- [ ] Remove dialog-specific logic from setProcessFinished method
- [ ] Remove dialog-specific logic from startAutoCloseTimer method
- [ ] Remove dialog-specific logic from focus method
- [ ] Update triggerAutoContinue to work with tool window context

## RunningCommandService Updates
- [ ] Remove MarkdownDialog references from RunningCommandService
- [ ] Update addRunningCommand to work with AiderOutputTab
- [ ] Update removeRunningCommand to work with AiderOutputTab
- [ ] Modify getRunningCommandsListModel to use appropriate type
- [ ] Update any dialog-specific functionality

## ShowLastCommandResultAction Fixes
- [ ] Remove MarkdownDialog creation from ShowLastCommandResultAction
- [ ] Update to use AiderOutputService for creating tool window output
- [ ] Ensure proper tool window activation and focus
- [ ] Remove activeDialog tracking mechanism
- [ ] Update to work with tool window tab system

## IDEBasedExecutor Updates
- [ ] Remove MarkdownDialog handling from IDEBasedExecutor
- [ ] Update to only work with tool window tabs
- [ ] Remove dialog-specific abort handling
- [ ] Update presentChanges method for tool window context

## MarkdownDialog Removal
- [ ] Delete MarkdownDialog.kt file entirely
- [ ] Remove all imports of MarkdownDialog from other files
- [ ] Remove MarkdownDialog references from CodingAiderOutputPresentation interface usage

## Tool Window Enhancements
- [ ] Ensure AiderOutputTab handles all former dialog functionality
- [ ] Verify auto-close behavior works appropriately for tool window (focus instead of close)
- [ ] Ensure abort functionality works correctly in tool window tabs
- [ ] Verify continue functionality works in tool window context
- [ ] Ensure create plan functionality works in tool window tabs

## Testing and Validation
- [ ] Test command execution with tool window output
- [ ] Test abort functionality in tool window
- [ ] Test continue plan functionality
- [ ] Test create plan functionality
- [ ] Test show last command result action
- [ ] Verify auto-continue works with tool window
- [ ] Test multiple concurrent commands in separate tabs
- [ ] Verify tool window persistence across IDE sessions

## Documentation and Cleanup
- [ ] Update any documentation referencing MarkdownDialog
- [ ] Remove unused imports and dependencies
- [ ] Clean up any remaining dialog-specific code paths
- [ ] Verify no broken references remain
