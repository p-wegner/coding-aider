# Coding Aider Actions Documentation

This document provides a comprehensive overview of the actions implemented in the Coding Aider plugin.

## Table of Contents

1. [AiderAction](#aideraction)
2. [AiderWebCrawlAction](#aiderwebcrawlaction)
3. [ApplyDesignPatternAction](#applydesignpatternaction)
4. [CommitAction](#commitaction)
5. [DocumentCodeAction](#documentcodeaction)
6. [FixCompileErrorAction](#fixcompileerroraction)
7. [OpenAiderActionGroup](#openaideractiongroup)
8. [PersistentFilesAction](#persistentfilesaction)
9. [SettingsAction](#settingsaction)
10. [ShowLastCommandResultAction](#showlastcommandresultaction)
11. [AiderClipboardImageAction](#aiderclipboardimageaction)
12. [RefactorToCleanCodeAction](#refactortocleancodeaction)
13. [DocumentEachFolderAction](#documenteachfolderaction)
14. [FixBuildAndTestErrorAction](#fixbuildandtesterroraction)

## AiderAction

`AiderAction` is the main action class for the Coding Aider plugin. It provides functionality to execute Aider commands
on selected files.

Key features:

- Supports both IDE-based and shell-based execution modes
- Uses `AiderInputDialog` for user input
- Handles persistent files
- Implements `ActionUpdateThread.BGT` for better performance

Exceptional implementation details:

- Supports a direct shell mode through the `AiderShellAction` subclass
- Uses a companion object to handle the main logic, allowing for easy reuse in other contexts
- Implements a flexible `executeAiderActionWithCommandData` method for custom command execution

## AiderWebCrawlAction

`AiderWebCrawlAction` is responsible for crawling web pages and processing them into markdown format.

Key features:

- Crawls a user-specified URL
- Converts HTML content to markdown using HtmlUnit and Flexmark
- Stores the result in a file with a unique name based on the URL and its content hash
- Optionally processes the markdown file further using an AI model
- Adds the processed file to persistent files

Exceptional implementation details:

- Uses MD5 hashing to generate unique filenames
- Implements a custom cleanup process using AI to simplify and structure the crawled content
- Provides user feedback through IDE notifications

## ApplyDesignPatternAction

`ApplyDesignPatternAction` allows users to apply design patterns to selected files or directories.

Key features:

- Presents a dialog for users to select and apply a predefined design pattern
- Loads design patterns from a YAML resource file
- Provides detailed tooltips for each design pattern
- Allows users to input additional context or instructions

## CommitAction

`CommitAction` provides a quick way to commit changes using Aider.

Key features:

- Executes the "/commit" command in Aider
- Uses IDE-based execution
- Automatically enables the "Yes" flag for streamlined operation

## DocumentCodeAction

`DocumentCodeAction` generates markdown documentation for selected files and directories.

Key features:

- Allows users to specify the output filename
- Generates documentation using Aider's AI capabilities
- Updates existing documentation files if they already exist
- Traverses directories to include all relevant files

## FixCompileErrorAction

`FixCompileErrorAction` provides functionality to fix compile errors using Aider.

Key features:

- Supports both interactive and quick fix modes
- Implements intention actions for IDE integration
- Detects compile errors in the current file
- Provides a background task for the interactive mode to prevent UI freezing

Exceptional implementation details:

- Uses `DocumentMarkupModel` to detect compile errors
- Implements two separate intention actions for quick fix and interactive modes

## OpenAiderActionGroup

`OpenAiderActionGroup` provides a popup menu with all available Aider actions.

Key features:

- Creates a flat list of all Aider actions, including those in subgroups
- Displays the actions in a popup menu
- Supports action search functionality

## PersistentFilesAction

`PersistentFilesAction` manages the list of persistent files for Aider.

Key features:

- Toggles files between persistent and non-persistent states
- Provides visual feedback through notifications
- Supports bulk operations on multiple files and directories

Exceptional implementation details:

- Dynamically updates the action text based on the current state of selected files
- Refreshes the context file to ensure UI updates

## SettingsAction

`SettingsAction` provides a quick way to open the Aider Settings dialog.

Key features:

- Opens the Aider Settings page in the IDE's settings dialog
- Implements `ActionUpdateThread.BGT` for better performance

## ShowLastCommandResultAction

`ShowLastCommandResultAction` displays the result of the last executed Aider command.

Key features:

- Retrieves the last command result from the AiderHistoryHandler
- Displays the result in a markdown dialog for easy reading

## AiderClipboardImageAction

`AiderClipboardImageAction` allows users to save images from the clipboard and add them to persistent files.

Key features:

- Detects images in the clipboard
- Saves the image to a unique file in the `.aider-docs/images` directory
- Automatically adds the saved image to persistent files

## RefactorToCleanCodeAction

`RefactorToCleanCodeAction` refactors code to adhere to clean code principles and SOLID principles.

Key features:

- Analyzes and refactors code for clean code and SOLID principles
- Provides detailed instructions for refactoring
- Ensures refactored code maintains original functionality

## DocumentEachFolderAction

`DocumentEachFolderAction` generates documentation for each folder in the selected files.

Key features:

- Processes each folder and generates markdown documentation
- Uses PlantUML to document dependencies and data flow
- Updates existing documentation files if they already exist

## FixBuildAndTestErrorAction

`FixBuildAndTestErrorAction` fixes build and test errors in the project.

Key features:

- Supports both interactive and quick fix modes
- Detects build and test errors
- Provides a background task for the interactive mode to prevent UI freezing

This documentation provides a comprehensive overview of the main actions in the Coding Aider plugin. Each action is
designed to enhance the development experience by integrating Aider's capabilities into the IDE workflow.
