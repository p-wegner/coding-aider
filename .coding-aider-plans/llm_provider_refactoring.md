[Coding Aider Plan]

# LLM Provider Logic Refactoring

## Overview

This plan outlines the refactoring of the LLM provider selection and API key management logic within the Coding Aider plugin. The goal is to create a more consistent and flexible system that allows custom providers to leverage globally configured API keys and treats built-in providers more uniformly with custom ones.

## Problem Description

Currently, the handling of LLM providers and their associated API keys is somewhat inconsistent.
1.  Custom providers require storing a specific API key within the plugin's credential store, even if a global key (e.g., `OPENAI_API_KEY` environment variable) is already available. This leads to redundancy and potential confusion.
2.  Built-in providers are handled separately from custom providers in several parts of the codebase (e.g., `ApiKeyChecker`, UI components), making the logic less unified and harder to extend.
3.  The `ApiKeyChecker`'s methods for checking availability and retrieving keys are not always clearly distinguishing between standard global keys and custom provider specific keys.

## Goals

1.  **Unified Provider Representation:** Represent both built-in and custom providers using a common structure (`LlmSelection` seems appropriate) throughout the application, especially in the UI and execution logic.
2.  **Flexible API Key Handling:** Allow custom providers to be configured to either use a specific API key stored for that provider *or* rely on a globally available API key (from environment variables, `.env` files, or the standard key in the credential store).
3.  **Consistent API Key Checking:** Refactor `ApiKeyChecker` to provide clear methods for checking key availability and retrieving the correct key value based on the selected provider (`LlmSelection`), considering both global and provider-specific keys.
4.  **Simplified UI:** Update the UI components (`AiderInputDialog`, `AiderSetupPanel`, `CustomLlmProviderDialog`/`EditorDialog`) to reflect the unified provider model and the flexible API key handling.
5.  **Robust Execution:** Ensure the execution strategies (`NativeAiderExecutionStrategy`, `DockerAiderExecutionStrategy`) correctly retrieve and pass the necessary API keys and model information based on the selected provider.

## Additional Notes and Constraints

*   The refactoring should minimize disruption to existing functionality.
*   API keys should continue to be stored securely using the Credential Store where applicable.
*   Compatibility with existing Aider CLI behavior should be maintained.
*   The `.env` file reading logic in `ApiKeyChecker` should be preserved for global keys.
*   The distinction between provider types (`LlmProviderType`) and their specific requirements (base URL, model prefix, auth type) must be maintained.

## References

*   [Checklist](llm_provider_refactoring_checklist.md)
*   [Context](llm_provider_refactoring_context.yaml)
<!-- SUBPLAN:llm_provider_refactoring_key_management -->
[Subplan: Core Key Management Refactoring](llm_provider_refactoring_key_management.md)
<!-- END_SUBPLAN -->
<!-- SUBPLAN:llm_provider_refactoring_ui -->
[Subplan: UI Refactoring](llm_provider_refactoring_ui.md)
<!-- END_SUBPLAN -->
<!-- SUBPLAN:llm_provider_refactoring_execution -->
[Subplan: Execution Strategy Integration](llm_provider_refactoring_execution.md)
<!-- END_SUBPLAN -->
