# Aider Actions Documentation

This document provides an overview of the actions implemented in the Aider plugin for IntelliJ IDEA.

## AiderAction

File: `src/main/kotlin/de/andrena/codingaider/actions/AiderAction.kt`

The `AiderAction` class is the main action for the Aider plugin. It allows users to interact with the Aider AI assistant directly from the IDE.

Key features:
- Supports both IDE-based and shell-based execution modes
- Collects files for context, including persistent files
- Uses a dialog for user input in non-shell mode

Notable methods:
- `executeAiderAction`: Main method to execute the Aider action
- `executeAiderActionWithCommandData`: Executes Aider with pre-configured command data
- `collectCommandData`: Gathers command data from the input dialog
- `collectDefaultCommandData`: Creates default command data for shell mode

The `AiderShellAction` is a subclass that always executes in shell mode.

## CommitAction

File: `src/main/kotlin/de/andrena/codingaider/actions/CommitAction.kt`

The `CommitAction` class is responsible for committing changes using Aider.

Key features:
- Executes the "/commit" command with Aider
- Uses IDE-based execution
- Automatically enables the "yes" flag

## SettingsAction

File: `src/main/kotlin/de/andrena/codingaider/actions/SettingsAction.kt`

The `SettingsAction` class opens the Aider Settings dialog.

Key features:
- Uses the IntelliJ IDEA settings framework to display Aider-specific settings

## DocumentCodeAction

File: `src/main/kotlin/de/andrena/codingaider/actions/DocumentCodeAction.kt`

The `DocumentCodeAction` class generates documentation for selected files or directories.

Key features:
- Prompts the user for a filename to store the documentation
- Uses Aider to generate markdown documentation
- Supports documentation of multiple files and directories

## AiderWebCrawlAction

File: `src/main/kotlin/de/andrena/codingaider/actions/AiderWebCrawlAction.kt`

The `AiderWebCrawlAction` class allows users to crawl web pages and process them using Aider.

Key features:
- Prompts the user for a URL to crawl
- Downloads and converts the web page to markdown
- Uses Aider to clean up and simplify the content
- Stores the processed content in a file and adds it to persistent files

Exceptional implementation details:
- Uses HtmlUnit for web crawling
- Implements MD5 hashing for unique filenames
- Optionally activates IDE executor after web crawl based on settings

## FixCompileErrorAction

File: `src/main/kotlin/de/andrena/codingaider/actions/FixCompileErrorAction.kt`

The `FixCompileErrorAction` class provides functionality to fix compile errors using Aider.

Key features:
- Detects compile errors in the current file
- Offers both quick fix and interactive modes
- Implements intention actions for easy access from the editor

Classes:
- `BaseFixCompileErrorAction`: Abstract base class with common functionality
- `FixCompileErrorAction`: Quick fix implementation
- `FixCompileErrorInteractive`: Interactive fix implementation

Both `FixCompileErrorAction` and `FixCompileErrorInteractive` have corresponding `Intention` classes for use as intention actions.

## PersistentFilesAction

File: `src/main/kotlin/de/andrena/codingaider/actions/PersistentFilesAction.kt`

The `PersistentFilesAction` class manages the addition and removal of files from the persistent files list.

Key features:
- Toggles files between persistent and non-persistent states
- Handles both individual files and directories
- Provides user feedback through notifications

## ShowLastCommandResultAction

File: `src/main/kotlin/de/andrena/codingaider/actions/ShowLastCommandResultAction.kt`

The `ShowLastCommandResultAction` class displays the result of the last Aider command.

Key features:
- Retrieves the last command result from the AiderHistoryHandler
- Displays the result in a markdown dialog

All actions implement the `getActionUpdateThread()` method to return `ActionUpdateThread.BGT`, ensuring that action updates are performed in the background thread for better performance.
