# [Coding Aider Plan - Checklist]

# Migrate to Tool Window Output System - Implementation Checklist

## Settings and Configuration
- [x] Update AiderDefaults.USE_TOOL_WINDOW_OUTPUT to default to true
- [ ] Remove dialog-specific settings from AiderSettings
- [ ] Remove enableMarkdownDialogAutoclose setting
- [ ] Remove markdownDialogAutocloseDelay setting
- [ ] Remove closeOutputDialogImmediately property
- [ ] Remove showMarkdownDevTools setting (move to general setting if needed)

## AiderOutputService Modifications
- [x] Remove createDialog method from AiderOutputService
- [x] Simplify createOutput to only create tool window tabs
- [x] Remove dialog-specific logic from updateProgress method
- [x] Remove dialog-specific logic from setProcessFinished method
- [x] Remove dialog-specific logic from startAutoCloseTimer method
- [x] Remove dialog-specific logic from focus method
- [x] Update triggerAutoContinue to work with tool window context

## RunningCommandService Updates
- [x] Remove MarkdownDialog references from RunningCommandService
- [x] Update addRunningCommand to work with AiderOutputTab
- [x] Update removeRunningCommand to work with AiderOutputTab
- [x] Modify getRunningCommandsListModel to use appropriate type
- [x] Update any dialog-specific functionality

## ShowLastCommandResultAction Fixes
- [x] Remove MarkdownDialog creation from ShowLastCommandResultAction
- [x] Update to use AiderOutputService for creating tool window output
- [x] Ensure proper tool window activation and focus
- [x] Remove activeDialog tracking mechanism
- [x] Update to work with tool window tab system

## IDEBasedExecutor Updates
- [x] Remove MarkdownDialog handling from IDEBasedExecutor
- [x] Update to only work with tool window tabs
- [x] Remove dialog-specific abort handling
- [ ] Update presentChanges method for tool window context

## MarkdownDialog Removal
- [x] Delete MarkdownDialog.kt file entirely
- [x] Remove all imports of MarkdownDialog from other files
- [x] Remove MarkdownDialog references from CodingAiderOutputPresentation interface usage

## Tool Window Enhancements
- [ ] Ensure AiderOutputTab handles all former dialog functionality
- [ ] Verify auto-close behavior works appropriately for tool window (focus instead of close)
- [ ] Ensure abort functionality works correctly in tool window tabs
- [ ] Verify continue functionality works in tool window context
- [ ] Ensure create plan functionality works in tool window tabs
