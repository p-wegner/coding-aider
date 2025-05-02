[Coding Aider Plan]

# LLM Provider Logic Refactoring - Core Key Management

## Overview

This subplan focuses on refactoring the core API key management and checking logic, primarily within `ApiKeyChecker` and potentially `ApiKeyManager`, to support the unified provider model and flexible key handling.

## Problem Description

The current `ApiKeyChecker` mixes concerns of checking for standard global keys (via env, .env, CredentialStore by key name) and interacting with custom provider keys (stored by provider name in CredentialStore via `ApiKeyManager`). This makes it difficult to consistently determine the correct API key value for a given `LlmSelection` which could be a built-in provider relying on a global key or a custom provider potentially using a provider-specific key.

## Goals

1.  **Clear Key Retrieval:** Implement a method in `ApiKeyChecker` that can retrieve the *value* of the primary API key required for a given `LlmSelection`, abstracting whether it's a global key or a provider-specific key.
2.  **Refined Availability Check:** Update the API key availability check method to work with `LlmSelection` and use the new retrieval logic.
3.  **Separate Global Key Lookup:** Ensure `ApiKeyChecker` retains a clear method for looking up standard global keys by their environment variable name.
4.  **Correct Docker Key Collection:** Verify and adjust the logic for collecting all necessary API keys and credentials for the Docker environment.
5.  **Focused `ApiKeyManager`:** Ensure `ApiKeyManager` remains focused on storing and retrieving keys specifically by the key name (for global keys) or provider name (for custom provider specific keys) in the Credential Store.

## Additional Notes and Constraints

*   The existing logic for reading `.env` files should be preserved for global keys.
*   The caching mechanism in `ApiKeyChecker` should be adapted or reviewed for the new retrieval logic.
*   Vertex AI credentials (file path, project ID, location) are not standard API keys and are stored directly in `CustomLlmProvider`. The key retrieval logic should handle this or the execution strategies should access these properties directly from the `LlmSelection.provider`. For this subplan, focus on the primary API key value; execution strategies will handle the rest.

## References

*   [Main Plan](../llm_provider_refactoring.md)
*   [Checklist](llm_provider_refactoring_key_management_checklist.md)
*   [Context](llm_provider_refactoring_key_management_context.yaml)
