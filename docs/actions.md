# Coding Aider Actions Documentation

This document provides an overview of the actions implemented in the Coding Aider plugin.

## Table of Contents
1. [AiderAction](#aideraction)
2. [AiderWebCrawlAction](#aiderwebcrawlaction)
3. [ApplyDesignPatternAction](#applydesignpatternaction)
4. [CommitAction](#commitaction)
5. [DocumentCodeAction](#documentcodeaction)
6. [FixCompileErrorAction](#fixcompileerroraction)
7. [PersistentFilesAction](#persistentfilesaction)
8. [SettingsAction](#settingsaction)
9. [ShowLastCommandResultAction](#showlastcommandresultaction)

## AiderAction

`AiderAction` is the main action class for the Coding Aider plugin. It provides functionality to execute Aider commands on selected files.

Key features:
- Supports both IDE-based and shell-based execution modes
- Uses `AiderInputDialog` for user input
- Handles persistent files

Exceptional implementation details:
- It supports a direct shell mode through the `AiderShellAction` subclass
- Uses a companion object to execute Aider actions, allowing for reuse in other parts of the plugin

## AiderWebCrawlAction

`AiderWebCrawlAction` is responsible for crawling web pages and processing them into markdown format.

Key features:
- Crawls a user-specified URL
- Converts HTML content to markdown using HtmlUnit and Flexmark
- Stores the result in a file with a unique name based on the URL and its content
- Optionally processes the markdown file further using an AI model
- Adds the processed file to persistent files

Exceptional implementation details:
- Uses MD5 hashing to generate unique filenames
- Implements a custom cleanup process using Aider's AI capabilities

## ApplyDesignPatternAction

`ApplyDesignPatternAction` allows users to apply design patterns to selected files or directories.

Key features:
- Loads design patterns from a YAML configuration file
- Presents a dialog for users to select a design pattern and provide additional information
- Generates instructions for Aider to apply the selected design pattern
- Supports both application and evaluation of design pattern applicability

Exceptional implementation details:
- Uses a custom dialog with tooltips for pattern descriptions
- Implements a flexible instruction generation system

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

## FixCompileErrorAction

`FixCompileErrorAction` provides functionality to fix compile errors using Aider.

Key features:
- Supports both interactive and quick fix modes
- Implements intention actions for IDE integration
- Retrieves compile errors from the IDE's markup model

Exceptional implementation details:
- Uses a background task to show the interactive dialog, preventing UI freezes
- Implements two separate intention actions for quick fix and interactive modes

## PersistentFilesAction

`PersistentFilesAction` manages the list of persistent files for Aider.

Key features:
- Toggles files between persistent and non-persistent states
- Provides visual feedback through notifications
- Supports bulk operations on multiple files and directories

## SettingsAction

`SettingsAction` provides a quick way to open the Aider Settings dialog.

## ShowLastCommandResultAction

`ShowLastCommandResultAction` displays the result of the last executed Aider command.

Key features:
- Retrieves the last command result from the AiderHistoryHandler
- Displays the result in a MarkdownDialog for easy reading

This documentation provides an overview of the main actions in the Coding Aider plugin. Each action is designed to enhance the development experience by integrating Aider's capabilities into the IDE workflow. The plugin offers a wide range of functionalities, from web crawling and design pattern application to compile error fixing and code documentation, all leveraging the power of AI-assisted coding.
