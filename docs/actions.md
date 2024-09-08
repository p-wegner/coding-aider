# Coding Aider Actions Documentation

This document provides an overview of the actions implemented in the Coding Aider plugin.

## Table of Contents
1. [AiderAction](#aideraction)
2. [AiderWebCrawlAction](#aiderwebcrawlaction)
3. [CommitAction](#commitaction)
4. [DocumentCodeAction](#documentcodeaction)
5. [FixCompileErrorAction](#fixcompileerroraction)
6. [PersistentFilesAction](#persistentfilesaction)
7. [SettingsAction](#settingsaction)
8. [ShowLastCommandResultAction](#showlastcommandresultaction)

## AiderAction

`AiderAction` is the main action class for the Coding Aider plugin. It provides functionality to execute Aider commands on selected files.

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
- Converts HTML content to markdown
- Stores the result in a file with a unique name based on the URL
- Optionally processes the markdown file further using an AI model

## CommitAction

`CommitAction` provides a quick way to commit changes using Aider.

Key features:
- Executes the "/commit" command in Aider
- Uses IDE-based execution

## DocumentCodeAction

`DocumentCodeAction` generates markdown documentation for selected files and directories.

Key features:
- Allows user to specify the output filename
- Generates documentation using Aider's AI capabilities

## FixCompileErrorAction

`FixCompileErrorAction` provides functionality to fix compile errors using Aider.

Key features:
- Supports both interactive and quick fix modes
- Implements intention actions for IDE integration

## PersistentFilesAction

`PersistentFilesAction` manages the list of persistent files for Aider.

Key features:
- Toggles files between persistent and non-persistent states
- Provides visual feedback through notifications

## SettingsAction

`SettingsAction` provides a quick way to open the Aider Settings dialog.

## ShowLastCommandResultAction

`ShowLastCommandResultAction` displays the result of the last executed Aider command.

This documentation provides an overview of the main actions in the Coding Aider plugin. Each action is designed to enhance the development experience by integrating Aider's capabilities into the IDE workflow.
