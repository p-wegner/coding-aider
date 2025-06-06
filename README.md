# Coding-Aider Plugin for IntelliJ IDEA <img src="src/main/resources/META-INF/pluginIcon.svg" alt="Coding-Aider Plugin Icon" width="40" height="40" style="vertical-align: middle;">

Seamlessly integrate Aider's AI-powered coding assistance directly into your IDE.
This integration boosts your productivity by offering rapid access for precision code generation and refactoring, all
while allowing you complete control over the context utilized by the LLM.

## Example Usage

![Aider Command Action Example](docs/AiderCommandAction.jpg)

## Important Note

To utilize this plugin, you need either:

1. A functional Aider installation (version 0.82.2 or higher):
   - On Mac/Linux: Ensure aider is in your PATH or set the path manually in Settings
   - On Windows: Aider should be available in PATH or set manually
   
2. OR Docker installed and running:
   - On Mac: Ensure docker command is in PATH (usually in /usr/local/bin)
   - On Windows: Docker Desktop should add docker to PATH automatically

### Mac Users

If you encounter "command not found" errors, you can:
- Set the full path to the aider executable in Tools > Aider Settings

## LLM Provider Configuration

Configure API Keys for LLM Providers for Aider to use in its [.env](https://aider.chat/docs/config/dotenv.html)
or [.yml](https://aider.chat/docs/config/aider_conf.html) files or by storing them safely in the plugin settings.

You can use:

- Standard OpenAI models
- Custom OpenAI-compatible API endpoints (like Azure, Anthropic, or local servers)
- Ollama models
- OpenRouter models
- Gemini models
- Vertex AI models (experimental)
- LM Studio models

### Custom LLM Provider Configuration

To configure a custom LLM provider:

1. Open Settings > Tools > Aider Settings
2. Click "Manage Providers..." in the Custom Providers section
3. Click "Add Provider" and configure:
   - Provider Name: A unique identifier for this provider
   - Provider Type: Select from OpenAI, Ollama, OpenRouter, Vertex AI, etc.
   - Base URL: For OpenAI-compatible APIs (e.g., http://localhost:8000/v1)
   - Model Name: The model to use (format depends on provider type)
   - API Key: If required by the provider type
   - Additional fields for specific providers (e.g., Project ID for Vertex AI)
4. The custom provider will appear in model selection dropdowns once configured

For OpenAI-compatible APIs, you can configure:
- Custom API base URL (e.g., http://localhost:8000/v1)
- Custom API key
- Model name (e.g., gpt-4, claude-3-opus)

For detailed configuration instructions, refer to
the [Aider documentation](https://aider.chat/docs/llms/openai-compat.html).

## Key Features

1. **AI-Powered Coding Assistance**: Harness the power of Aider to receive intelligent coding assistance
   within your IDE. See [Plan Mode](docs/plan-mode.md) for details on maintaining context across sessions.

2. **Intuitive Access**:
    - Quickly initiate Aider actions via the "Start Aider Action" option in the Tools menu or Project View popup menu.
    - Use the keyboard shortcut Alt+A for rapid access.
    - Automatically commit all changes with an LLM-generated message using ALT + D

3. **Persistent File Management**: 
   - Manage frequently used files for persistent context for Aider operations with Alt+Shift+A
   - Temporarily stash files to different contexts and restore them when needed
   - Organize different file sets for different tasks

4. **Dual Execution Modes**:
    - IDE-based execution for seamless integration.
    - Shell-based execution
        - for users who prefer Aider's rich terminal interaction
        - useful for easy context setup in the IDE
        - combinable with docker-based execution to simplify file system mounting

5. **Git Integration**: Automatically launch a Git comparison tool post-Aider operations for easy change review.

6. **Real-time Progress Tracking**: Monitor Aider command progress through an embedded browser-based Markdown viewer.

7. **Multi-File Support**: Execute Aider actions on multiple files or directories while controlling the context provided
   to Aider from your IDE.

8. **Webcrawl (Experimental)**: Download and convert pages to markdown stored in a .aider-docs folder to add to context.

9. **Clipboard Support**: 
   - **Clipboard Image Support**: Save clipboard images directly to the project and add them to persistent files.
   - **Clipboard Edit Format**: Apply code changes from clipboard containing SEARCH/REPLACE blocks or other edit formats.

10. **Refactor to Clean Code Action**: Refactor code to adhere to clean code principles.

11. **Fix Build and Test Error Action**: Fix build and test errors in the project.

12. **Various Specialized Actions**:
    - **Commit Action**: Quickly commit changes using Aider.
    - **Document Code Action**: Generate markdown documentation for selected files and directories.
    - **Fix Compile Error Action**: Fix compile errors using Aider's AI capabilities.
    - **Show Last Command Result Action**: Display the result of the last executed Aider command.
    - **Settings Action**: Quickly access Aider Settings.
    - **Apply Design Pattern Action**: Apply predefined design patterns to selected files or directories.
    - **Persistent Files Action**: Manage the list of persistent files for Aider operations.
    - **OpenAiderActionGroup**: Access a popup menu with all available Aider actions.
    - **Document Each Folder Action**: Generate documentation for each folder in the selected files.
    - **Continue Plan Action**: Allow users to select and continue an unfinished plan within the Coding Aider plugin.
13. **Working Directory Support**:
    - Configure a specific working directory for Aider operations
    - Files outside the working directory are not included in the context and won't be seen by Aider
    - Might help working with large repositories
14. **Structured Mode and Plans**: 
    - Break down complex features into manageable tasks
    - Track implementation progress with checklists
    - Maintain context across multiple coding sessions
    - See [Plan Mode](docs/plan-mode.md) for detailed workflow
15. **Summarized Output**:
    - Option to enable structured XML summaries of Aider's changes
    - Intention and Summary blocks are included in the output
16. **Aider Commands and Arguments**:
    - If needed, you can specify additional [arguments](https://aider.chat/docs/config/options.html) for the Aider
      command in the settings or in the command window.

For a detailed description of all available actions, please refer to the [Actions Documentation](docs/actions.md).

## Advantages Over Other Coding Assistant Plugins

Coding-Aider addresses limitations in existing IntelliJ plugins, particularly for tasks involving multiple file creation
or modification. Aider's unique capabilities include:

1. Optimized token usage for improved speed (featuring replace edit mode, repo-map, and context control).
2. A feature-rich terminal interface for command-line enthusiasts.
3. An extensive range of commands to automate common development tasks.
4. Robust recovery mechanisms with seamless Git integration.

Coding-Aider brings these powerful terminal-based features directly into your IDE, leveraging established IDE
functionalities like Git integration and keyboard shortcuts.

## Getting Started

1. Install the Coding-Aider plugin in a compatible JetBrains IDE.
2. (**Can be skipped if aider in docker is used**) Install Aider-Chat :
    - Visit https://aider.chat/
    - Install as a global pipx Python app
    - Ensure it's accessible from your terminal (`aider --help`)
3. Configure Aider settings:
    - Navigate to Tools > Aider Settings
    - Select Docker-based Aider if aider is not installed globally
    - Run "Test Aider installation" to verify your global aider installation (or run the aider docker container if
      docker is used). First time running the test
      with docker will take a while as the docker image (1GB) is downloaded.
    - (Optional) Globally configure API keys for LLM Providers you plan to use with aider (
      see https://aider.chat/docs/config/dotenv.html)
4. Configure LLM Providers:
    - Navigate to Tools > Aider Settings
    - Add your API keys for the LLM Providers you wish to use (if not globally configured)
    - Configure the default LLM Provider for your Aider actions

5. Basic Usage:
    - Select files or directories in your project
    - Use Alt+A or use the entries in the right-click context menu to initiate an Aider action
    - Enter your coding request in the dialog
    - Review Aider's output and resulting changes in your project

6. Advanced Usage:
    - Use Alt+Shift+Q to quickly access all available Aider actions
    - Use Alt+Shift+A to manage persistent files for context
    - Utilize specialized actions like Document Code, Fix Compile Error, or Web Crawl for specific tasks
    - Access the last command result using the Show Last Command Result action

## Settings Overview

![Aider Command Action Example](docs/settings1.jpg)
![Aider Command Action Example](docs/settings2.jpg)
