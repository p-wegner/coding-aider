# Overview of Coding Aider Modules

## Actions Module
The Aider Actions module provides a set of actions that enhance the functionality of the Aider tool within the IDE. Key classes include:
- **SettingsAction**: Opens the Aider settings dialog.
- **PersistentFilesAction**: Manages files that should persist across sessions.
- **ShowLastCommandResultAction**: Displays the result of the last command executed by Aider.
- **CommitAction**: Commits changes to the version control system.

[Link to Actions Documentation](src/main/kotlin/de/andrena/codingaider/actions/actions.md)

## Command Module
The Command module defines the data structures and interfaces required to execute commands in the Aider application. It includes:
- **FileData**: Represents a file's metadata.
- **CommandData**: Encapsulates the necessary information for command execution.

[Link to Command Documentation](src/main/kotlin/de/andrena/codingaider/command/command.md)

## Docker Module
The Docker module manages Docker containers within the application. It includes:
- **DockerContainerManager**: Responsible for managing Docker containers, including stopping and removing them.

[Link to Docker Documentation](src/main/kotlin/de/andrena/codingaider/docker/docker.md)

## Executors Module
The Executors module is responsible for executing commands related to the Aider application. Key classes include:
- **CommandExecutor**: Executes commands based on provided data.
- **ShellExecutor**: Executes commands in a shell environment.

[Link to Executors Documentation](src/main/kotlin/de/andrena/codingaider/executors/executors.md)

## Input Dialog Module
The Input Dialog module provides a user interface for interacting with coding aids. Key classes include:
- **AiderInputDialog**: Represents the main dialog for user input.
- **AiderContextView**: Manages the context view of files.

[Link to Input Dialog Documentation](src/main/kotlin/de/andrena/codingaider/inputdialog/inputdialog.md)

## Messages Module
The Messages module handles events related to persistent file changes within the application. It provides a mechanism for components to subscribe to notifications when persistent files are modified.

[Link to Messages Documentation](src/main/kotlin/de/andrena/codingaider/messages/messages.md)

## Output View Module
The Output View module is responsible for displaying output in a user-friendly manner. It includes:
- **MarkdownDialog**: Displays markdown content and allows user interaction.

[Link to Output View Documentation](src/main/kotlin/de/andrena/codingaider/outputview/outputview.md)

## Services Module
The Services module provides services for managing user interactions, file handling, and token counting. Key classes include:
- **AiderHistoryService**: Manages input and chat history.
- **TokenCountService**: Counts tokens in text and files.

[Link to Services Documentation](src/main/kotlin/de/andrena/codingaider/services/services.md)

## Settings Module
The Settings module manages project settings and configurations related to the Aider tool. It includes default settings and user interface components for configuration.

[Link to Settings Documentation](src/main/kotlin/de/andrena/codingaider/settings/settings.md)

## Tool Window Module
The Tool Window module provides a user interface component for managing persistent files within the Coding Aider application.

[Link to Tool Window Documentation](src/main/kotlin/de/andrena/codingaider/toolwindow/toolwindow.md)

## Utils Module
The Utils module provides utility classes and functions for file handling, Git interactions, and API key management.

[Link to Utils Documentation](src/main/kotlin/de/andrena/codingaider/utils/utils.md)
# Overview of Coding Aider Modules

## Actions Module
The Aider Actions module provides a set of actions that enhance the functionality of the Aider tool within the IDE. Key classes include:
- **SettingsAction**: Opens the Aider settings dialog.
- **PersistentFilesAction**: Manages files that should persist across sessions.
- **ShowLastCommandResultAction**: Displays the result of the last command executed by Aider.
- **CommitAction**: Commits changes to the version control system.

[Link to Actions Documentation](src/main/kotlin/de/andrena/codingaider/actions/actions.md)

## Command Module
The Command module defines the data structures and interfaces required to execute commands in the Aider application. It includes:
- **FileData**: Represents a file's metadata.
- **CommandData**: Encapsulates the necessary information for command execution.

[Link to Command Documentation](src/main/kotlin/de/andrena/codingaider/command/command.md)

## Docker Module
The Docker module manages Docker containers within the application. It includes:
- **DockerContainerManager**: Responsible for managing Docker containers, including stopping and removing them.

[Link to Docker Documentation](src/main/kotlin/de/andrena/codingaider/docker/docker.md)

## Executors Module
The Executors module is responsible for executing commands related to the Aider application. Key classes include:
- **CommandExecutor**: Executes commands based on provided data.
- **ShellExecutor**: Executes commands in a shell environment.

[Link to Executors Documentation](src/main/kotlin/de/andrena/codingaider/executors/executors.md)

## Input Dialog Module
The Input Dialog module provides a user interface for interacting with coding aids. Key classes include:
- **AiderInputDialog**: Represents the main dialog for user input.
- **AiderContextView**: Manages the context view of files.

[Link to Input Dialog Documentation](src/main/kotlin/de/andrena/codingaider/inputdialog/inputdialog.md)

## Messages Module
The Messages module handles events related to persistent file changes within the application. It provides a mechanism for components to subscribe to notifications when persistent files are modified.

[Link to Messages Documentation](src/main/kotlin/de/andrena/codingaider/messages/messages.md)

## Output View Module
The Output View module is responsible for displaying output in a user-friendly manner. It includes:
- **MarkdownDialog**: Displays markdown content and allows user interaction.

[Link to Output View Documentation](src/main/kotlin/de/andrena/codingaider/outputview/outputview.md)

## Services Module
The Services module provides services for managing user interactions, file handling, and token counting. Key classes include:
- **AiderHistoryService**: Manages input and chat history.
- **TokenCountService**: Counts tokens in text and files.

[Link to Services Documentation](src/main/kotlin/de/andrena/codingaider/services/services.md)

## Settings Module
The Settings module manages project settings and configurations related to the Aider tool. It includes default settings and user interface components for configuration.

[Link to Settings Documentation](src/main/kotlin/de/andrena/codingaider/settings/settings.md)

## Tool Window Module
The Tool Window module provides a user interface component for managing persistent files within the Coding Aider application.

[Link to Tool Window Documentation](src/main/kotlin/de/andrena/codingaider/toolwindow/toolwindow.md)

## Utils Module
The Utils module provides utility classes and functions for file handling, Git interactions, and API key management.

[Link to Utils Documentation](src/main/kotlin/de/andrena/codingaider/utils/utils.md)
