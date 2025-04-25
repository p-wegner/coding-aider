[Coding Aider Plan - Checklist]

- [ ] Modify `ApiKeyChecker` to introduce a method `getApiKeyForLlm(llmSelection: LlmSelection)` that retrieves the *value* of the primary API key based on whether it's a built-in or custom provider and its type.
- [ ] Update `isApiKeyAvailableForLlm(llm: String)` to accept `LlmSelection` and use the new retrieval logic.
- [ ] Refactor `getApiKeyValue(apiKeyName: String)` to focus *only* on retrieving standard global keys (from env, .env, CredentialStore by key name).
- [ ] Ensure `getApiKeysForDocker` correctly collects both standard global keys and custom provider specific keys/credentials needed for Docker.
- [ ] Review `ApiKeyManager` methods to ensure they are only used for storing/retrieving custom provider specific keys by provider name.
- [ ] Add tests for new key retrieval logic in `ApiKeyChecker`.
