[Coding Aider Plan - Checklist]

- [ ] Update `LlmSelection` to clearly indicate if it's a built-in or custom provider.
- [ ] Modify `AiderInputDialog`'s LLM combobox to display providers consistently using `LlmSelection` and update token count based on the selected provider.
- [ ] Update `AiderSetupPanel`'s API Key section to reflect the new key management logic (e.g., indicate if a global key is found for standard providers, allow saving specific keys for custom providers if needed, potentially link to custom provider editor).
- [ ] Update `CustomLlmProviderEditorDialog` to handle API key input/display based on whether the provider type supports/requires a specific key or relies on a global key, and use `ApiKeyManager` correctly.
- [ ] Ensure `CustomLlmProviderDialog` correctly displays provider information and enables/disables buttons based on provider type (built-in vs. custom).
