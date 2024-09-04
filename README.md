# Coding-Aider Plugin

Coding-Aider is an IntelliJ IDEA plugin that integrates Aider support directly into your IDE, enhancing your development
experience with AI-powered coding assistance.

## Main Features

1. **AI-Powered Coding Assistance**: Leverage the power of Aider to get intelligent coding suggestions and assistance
   right within your IDE.

2. **Easy Access**: Start Aider actions quickly using the "Start Aider Action" option in the Tools menu or Project View
   popup menu.

3. **Keyboard Shortcuts**: Use Alt+A to quickly invoke the Aider action.

4. **Persistent File Management**: Add or remove files from a persistent list for frequent Aider operations using
   Alt+Shift+A.

5. **Flexible Execution Modes**:
    - IDE-based execution for seamless integration.
    - Shell-based execution for users who prefer aiders rich terminal interaction.

6. **Git Integration**: Automatically opens a Git comparison tool after Aider operations to review changes.

7. **Progress Tracking**: View real-time progress of Aider commands in a Markdown dialog.

8. **Multi-File Support**: Perform Aider actions on multiple files or directories and control the context aider gets
   from your IDE.

## Why Yet Another Coding Assistant Plugin?

Existing Intellij plugins fail to streamline common development tasks when creation or modification of multiple files is
required.
Aider offers a unique AI-powered coding assistant:

1. optimization of token usage and therefore speed (replace edit mode, repo-map, context control, ...)
2. a rich terminal interface for users who prefer it
3. a wide range of commands that can be used to automate common development tasks
4. solid recovery mechanisms for when things go wrong with seamless git integration

But ... Aider is a terminal-based tool, and we want to bring its power directly into your IDE and use established IDE
features like Git integration, keyboard shortcuts and more.

## Getting Started

1. Install Aider-Chat https://aider.chat/ as a global pipx python app and ensure it is accessible from your terminal (
   aider --help).
2. Install the Coding-Aider plugin in your IntelliJ IDEA.
3. Configure the Aider settings in Tools > Aider Settings.
4. Select files or directories in your project.
5. Use Alt+A or right-click to start an Aider action.
6. Enter your coding request in the dialog that appears.
7. Review the Aider output and resulting changes in your project.

Enhance your coding workflow with AI-assisted development using Coding-Aider!
