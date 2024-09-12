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

## AiderAction

`AiderAction` is the main action class for the Coding Aider plugin. It provides functionality to execute Aider commands
on selected files.

Key features:

- Supports both IDE-based and shell-based execution modes
- Uses `AiderInputDialog` for user input
- Handles persistent files

Exceptional implementation details:

- It supports a direct shell mode through the `AiderShellAction` subclass

## AiderWebCrawlAction

`AiderWebCrawlAction` is responsible for crawling web pages and processing them into markdown format.

Key features:

- Crawls a user-specified URL
- Converts HTML content to markdown using HtmlUnit and Flexmark
- Stores the result in a file with a unique name based on the URL and its content
- Optionally processes the markdown file further using an AI model
- Adds the processed file to persistent files

## ApplyDesignPatternAction

`ApplyDesignPatternAction` allows users to apply design patterns to selected files or directories.

Key features:

- Presents a dialog for users to apply a predefined design pattern
- The model will check if the pattern is applicable before applying it

## CommitAction

`CommitAction` provides a quick way to commit changes using Aider.

Key features:

- Executes the "/commit" command in Aider
- Uses IDE-based execution

## DocumentCodeAction

`DocumentCodeAction` generates markdown documentation for selected files and directories.

Key features:

- Allows users to specify the output filename
- Generates documentation using Aider's AI capabilities
- Updates existing documentation files if they already exist

## FixCompileErrorAction

`FixCompileErrorAction` provides functionality to fix compile errors using Aider.

Key features:

- Supports both interactive and quick fix modes
- Implements intention actions for IDE integration

## OpenAiderActionGroup

`OpenAiderActionGroup` provides a popup menu with all available Aider actions.

## PersistentFilesAction

`PersistentFilesAction` manages the list of persistent files for Aider.

Key features:

- Toggles files between persistent and non-persistent states
- Provides visual feedback through notifications

## SettingsAction

`SettingsAction` provides a quick way to open the Aider Settings dialog.

## ShowLastCommandResultAction

`ShowLastCommandResultAction` displays the result of the last executed Aider command.

This documentation provides a comprehensive overview of the main actions in the Coding Aider plugin. Each action is
designed to enhance the development experience by integrating Aider's capabilities into the IDE workflow. 
