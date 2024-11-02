# Docker Container Manager

## Overview
The `DockerContainerManager` class is responsible for managing Docker containers within the application. It provides functionality to retrieve the container ID from a temporary file, stop the running Docker container, and remove the temporary file after use.

## Key Classes and Methods

### DockerContainerManager
- **Properties:**
  - `logger`: Logger instance for logging messages.
  - `uniqueId`: A unique identifier for the instance.
  - `cidFile`: A temporary file that stores the Docker container ID.
  - `dockerContainerId`: The ID of the Docker container.

- **Public Methods:**
  - `getCidFilePath()`: Returns the absolute path of the CID file.
  - `stopDockerContainer()`: Stops the Docker container using the container ID.
  - `removeCidFile()`: Deletes the CID file if it exists.

- **Private Methods:**
  - `getDockerContainerId()`: Reads the Docker container ID from the CID file, retrying up to 10 times if necessary.

## Design Patterns
The class follows the Singleton pattern for the logger instance and uses the Command pattern for executing the Docker commands.

## Dependencies
- `com.intellij.openapi.diagnostic.Logger`: For logging purposes.
- `java.io.File`: For file operations.
- `java.util.UUID`: For generating unique identifiers.
- `java.util.concurrent.TimeUnit`: For managing timeouts.

## Data Flow
1. The `DockerContainerManager` class creates a temporary file to store the Docker container ID.
2. It attempts to read the container ID from the file.
3. If the container ID is retrieved successfully, it can stop the container using the `stopDockerContainer()` method.
4. After stopping the container, the CID file is removed.

## Usage
To use the `DockerContainerManager`, create an instance and call the appropriate methods to manage Docker containers.

## Exception Handling
The class includes exception handling to log errors when stopping the Docker container or removing the CID file.

