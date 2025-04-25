[Coding Aider Plan]

# LLM Provider Logic Refactoring - UI Refactoring

## Overview

This subplan focuses on updating the user interface components to correctly display and interact with the unified LLM provider model (`LlmSelection`) and the refined API key management logic.

## Problem Description

The current UI components (`AiderInputDialog`, `AiderSetupPanel`, `CustomLlmProviderDialog`/`EditorDialog`) have logic tied to the previous distinction between built-in and custom providers and the specific way API keys were handled. They need to be updated to consistently use `LlmSelection` and reflect whether a provider uses a global key or a provider-specific key.

## Goals

1.  **Consistent LLM Selection:** Modify `AiderInputDialog` to use `LlmSelection` in its combobox and ensure it correctly displays both built-in and custom providers.
2.  **Updated API Key Display:** Refactor `AiderSetupPanel`'s API Key section to clearly indicate the status of standard global API keys and potentially link to or integrate with custom provider management for provider-specific keys.
3.  **Refined Custom Provider Dialogs:** Update `CustomLlmProviderEditorDialog` to handle API key input/display based on the selected `LlmProviderType`, allowing configuration for provider-specific keys where applicable and indicating reliance on global keys otherwise.
4.  **Correct Provider Listing:** Ensure `CustomLlmProviderDialog` correctly lists both built-in and custom providers, displaying relevant information and enabling/disabling actions appropriately.

## Additional Notes and Constraints

*   The visual distinction between built-in and custom providers in the UI should be maintained for clarity.
*   The secure handling of API key input (masking) must be preserved.
*   The logic for hiding providers (`DefaultProviderSettings`) should continue to function correctly.

## References

*   [Main Plan](../llm_provider_refactoring.md)
*   [Checklist](llm_provider_refactoring_ui_checklist.md)
*   [Context](llm_provider_refactoring_ui_context.yaml)
