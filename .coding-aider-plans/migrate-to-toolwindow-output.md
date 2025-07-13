# [Coding Aider Plan]

# Migrate to Tool Window Output System

## Overview

This plan involves migrating the Aider plugin from using popup MarkdownDialog windows to exclusively using the tool window output system. The tool window approach provides a superior user experience with better integration into the IDE, persistent output history, and improved workflow management.

## Problem Description

Currently, the Aider plugin supports both MarkdownDialog popups and tool window tabs for displaying command output. The dual approach creates complexity and maintenance overhead. The MarkdownDialog approach has several limitations:

1. **Poor Integration**: Popup dialogs don't integrate well with the IDE's window management
2. **Limited Persistence**: Dialog content is lost when closed
3. **Workflow Disruption**: Popups can interfere with normal IDE usage
4. **Maintenance Overhead**: Supporting two different output systems increases code complexity

The tool window system is superior because it:
- Provides persistent output history
- Integrates seamlessly with IDE window management
- Supports multiple concurrent outputs via tabs
- Offers better user experience for long-running operations

## Goals

1. **Remove MarkdownDialog**: Eliminate the MarkdownDialog class and all related popup functionality
2. **Default to Tool Window**: Update settings to use tool window output by default
3. **Maintain Functionality**: Ensure all existing features work with tool window output
4. **Update Services**: Modify RunningCommandService to work with tool window tabs instead of dialogs
5. **Fix ShowLastCommandResultAction**: Ensure the "Show Last Command Result" action works with tool window system
6. **Clean Up Settings**: Remove obsolete settings related to dialog-specific features

## Additional Notes and Constraints

- Must maintain backward compatibility for existing user workflows
- All existing functionality (abort, continue, create plan, dev tools) must work in tool window
- Auto-close behavior should be adapted for tool window context (focus/activate instead of close)
- The migration should be seamless for end users
- Performance should be maintained or improved

## References

- `AiderOutputService`: Central service managing output creation and display
- `MarkdownDialog`: Current popup dialog implementation to be removed
- `AiderOutputTab`: Tool window tab implementation to become primary
- `RunningCommandService`: Service tracking running commands, needs adaptation
- `ShowLastCommandResultAction`: Action for showing last command result
- `AiderSettings`: Configuration management, needs cleanup
