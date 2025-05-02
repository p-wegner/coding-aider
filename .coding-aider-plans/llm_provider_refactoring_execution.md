[Coding Aider Plan]

# LLM Provider Logic Refactoring - Execution Strategy Integration

## Overview

This subplan focuses on integrating the refactored LLM provider and API key logic into the execution strategies (`NativeAiderExecutionStrategy`, `DockerAiderExecutionStrategy`) to ensure Aider is invoked correctly with the chosen model and necessary credentials.

## Problem Description

The current execution strategies retrieve API keys and determine model arguments based on the previous, less unified logic. They need to be updated to work with the `LlmSelection` object and the refactored `ApiKeyChecker` methods to correctly configure the environment and command line arguments for Aider, supporting both built-in and custom providers with their potentially different key requirements and model naming conventions.

## Goals

1.  **Correct Model Argument:** Ensure `AiderExecutionStrategy.buildCommonArgs` correctly formats the `--model` argument based on the selected `LlmSelection`, including handling provider-specific prefixes where required.
2.  **Native Environment Setup:** Update `NativeAiderExecutionStrategy.setApiKeyEnvironmentVariables` to retrieve the correct API key(s) and other credentials (like Vertex AI project/location) using the refactored `ApiKeyChecker` and `LlmSelection.provider` properties, setting the appropriate environment variables for Aider.
3.  **Docker Environment Setup:** Update `DockerAiderExecutionStrategy.buildCommand` to retrieve the correct API key(s) and other credentials, setting environment variables and potentially mounting necessary files (like Vertex AI credentials) for the Docker container.
4.  **Handle Provider Types:** Ensure both strategies correctly handle the specific requirements of different `LlmProviderType`s when setting up the environment and command.

## Additional Notes and Constraints

*   The logic for mounting the project directory and `.aider.conf.yml` in Docker should be preserved.
*   The handling of common arguments like `--yes`, `--edit-format`, `--lint-cmd`, etc., should remain unchanged.
*   The logic for handling auto-commits and dirty-commits should remain unchanged.

## References

*   [Main Plan](../llm_provider_refactoring.md)
*   [Checklist](llm_provider_refactoring_execution_checklist.md)
*   [Context](llm_provider_refactoring_execution_context.yaml)
