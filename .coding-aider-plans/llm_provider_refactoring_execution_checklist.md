[Coding Aider Plan - Checklist]

- [ ] Update `AiderExecutionStrategy.buildCommonArgs` to correctly determine the model argument based on the selected `LlmSelection` (built-in or custom) and its type.
- [ ] Update `NativeAiderExecutionStrategy.setApiKeyEnvironmentVariables` to retrieve the correct API key(s) and other credentials based on the selected `LlmSelection` and its type, using the refactored `ApiKeyChecker` and accessing `LlmSelection.provider` properties.
- [ ] Update `DockerAiderExecutionStrategy.buildCommand` to retrieve the correct API key(s) and potentially mount credentials based on the selected `LlmSelection` and its type, using the refactored `ApiKeyChecker` and accessing `LlmSelection.provider` properties.
- [ ] Ensure environment variables are set correctly for different provider types in both native and Docker strategies.
