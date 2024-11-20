# Coding-Aider Plugin for IntelliJ IDEA <img src="src/main/resources/META-INF/pluginIcon.svg" alt="Coding-Aider Plugin Icon" width="40" height="40" style="vertical-align: middle;">

Seamlessly integrate Aider's AI-powered coding assistance directly into your IDE.
This integration boosts your productivity by offering rapid access for precision code generation and refactoring, all
while allowing you complete control over the context utilized by the LLM.

## Example Usage

![Aider Command Action Example](docs/AiderCommandAction.jpg)

## Important Note

To utilize this plugin, you

- must have a functional Aider installation (version 0.61.0 or higher)
- or be able to run docker containers

## LLM Provider Configuration
Configure API Keys for LLM Providers for Aider to use in its [.env](https://aider.chat/docs/config/dotenv.html) or [.yml](https://aider.chat/docs/config/aider_conf.html) files or by storing them safely in the plugin settings.

You can use:
- Standard OpenAI models
- Custom OpenAI-compatible API endpoints (like Azure, Anthropic, or local servers)
- Ollama models

### Custom Model Configuration
To configure a custom OpenAI-compatible model:

1. Open Settings > Tools > Aider Settings
2. In the "Custom Model" section:
   - Enter the API Base URL (e.g., http://localhost:8000/v1)
   - Enter the Model Name with "openai/" prefix (e.g., openai/gpt-4)
   - Enter your API Key
3. The custom model will appear in model selection dropdowns once configured

For custom OpenAI-compatible APIs, you can configure:
- Custom API base URL (e.g., http://localhost:8000/v1)
- Custom API key
- Model name prefixed with "openai/" (e.g., openai/gpt-4)

For detailed configuration instructions, refer to the [Aider documentation](https://aider.chat/docs/llms/openai-compat.html).

## Key Features

1. **AI-Powered Coding Assistance**: Harness the power of Aider to receive intelligent coding assistance
   within your IDE.

2. **Intuitive Access**:
    - Quickly initiate Aider actions via the "Start Aider Action" option in the Tools menu or Project View popup menu.
    - Use the keyboard shortcut Alt+A for rapid access.
    - Automatically commit all changes with an LLM-generated message using ALT + D

3. **Persistent File Management**: Manage frequently used files for persistent context for Aider operations with
   Alt+Shift+A,
   within the Aider Command Window or the Aider Settings.

4. **Dual Execution Modes**:
    - IDE-based execution for seamless integration.
    - Shell-based execution
      - for users who prefer Aider's rich terminal interaction 
      - useful for easy context setup in the IDE 
      - combinable with docker-based execution to simplify file system mounting

5. **Git Integration**: Automatically launch a Git comparison tool post-Aider operations for easy change review.

6. **Real-time Progress Tracking**: Monitor Aider command progress through a custom Markdown viewer.

7. **Multi-File Support**: Execute Aider actions on multiple files or directories while controlling the context provided
   to Aider from your IDE.

8. **Webcrawl (Experimental)**: Download and convert pages to markdown stored in a .aider-docs folder to add to context.

9. **Clipboard Image Support**: Save clipboard images directly to the project and add them to persistent files.

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

13. **Structured Mode**:
    - Facilitates a more organized approach to developing features and implementing code changes.
    - Simply describe a feature briefly, and the plugin will generate a detailed plan and checklist for you.
    - These plans and checklists are saved in the .coding-aider-plans directory and can be manually adjusted as needed.
    - Once a plan is created, Aider will automatically update it to reflect current progress and any new requirements.
    - This mode aids in tracking the implementation progress of features and avoids time-consuming manual prompting for larger features.
    - If a plan is active (i.e. its files are contained in the context), you can leave the message field empty, and Aider will continue working on the existing plan.
    - Alternatively, you can use the [Continue Plan](./docs/actions.md) action to continue working on the existing plan.
    - The Tool Window will display an overview of existing plans and their progress.
    - Further insights can be gained by reviewing the used system prompt in [AiderPlanPromptService](./src/main/kotlin/de/andrena/codingaider/services/plans/AiderPlanPromptService.kt).
14. **Aider Commands and Arguments**:
    - If needed, you can specify additional [arguments](https://aider.chat/docs/config/options.html) for the Aider command in the settings or in the command window.

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
