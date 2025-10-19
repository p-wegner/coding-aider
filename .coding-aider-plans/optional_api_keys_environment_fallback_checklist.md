[Coding Aider Plan - Checklist]

# Implementation Checklist - Optional API Keys with Environment Variable Fallback

## LlmProviderType Enhancements
- [x] Add `apiKeyOptional` flag to all relevant provider types (OPENAI, OPENROUTER, GEMINI)
- [x] Define default environment variable names for each provider type
- [x] Enhance `getApiKeyName()` method to return default environment variable names
- [x] Update provider documentation with environment variable requirements

## CustomLlmProvider Class Updates
- [x] Add optional API key field with null/empty support
- [x] Add field to track whether API key was explicitly provided
- [x] Add validation for environment variable fallback logic
- [x] Update serialization/deserialization to handle optional keys

## ApiKeyChecker Enhancements
- [x] Modify `isApiKeyAvailableForLlm()` to handle optional API keys
- [x] Add method to check environment variable availability specifically
- [x] Enhance `getApiKeyForLlm()` to support environment variable fallback
- [x] Update key retrieval logic for fallback chain: provider key → environment variable → default provider variable
- [x] Update `getAllLlmOptions()` to reflect API key availability with environment fallbacks
- [x] Enhance `getApiKeysForDocker()` to handle environment variable passthrough
- [x] Update caching mechanism to work with environment variable lookups

## CustomLlmProviderEditorDialog Updates
- [x] Modify validation logic to allow empty API keys for optional providers
- [x] Add visual indicators (icons/colors) for optional vs required API keys
- [x] Implement real-time environment variable validation and availability checking
- [x] Update tooltips to explain environment variable fallback behavior
- [x] Add environment variable name field for custom variable specification
- [x] Update provider type selection to show API key requirements clearly
- [x] Enhance error messages with environment variable guidance

## Environment Variable Management
- [x] Create utility class for environment variable management
- [x] Implement caching for environment variable lookups
- [x] Add support for custom environment variable names
- [x] Handle environment variable precedence and override rules
- [x] Add validation for environment variable format and accessibility

## UI/UX Improvements
- [x] Design and implement visual indicators for API key status
- [x] Create tooltips and inline help for environment variable usage
- [x] Update settings panels to show API key source (stored/environment/not available)
- [x] Add configuration examples for common provider setups
- [x] Implement real-time feedback for environment variable availability

## Integration with Execution Strategies
- [x] Update NativeAiderExecutionStrategy to handle optional API keys
- [x] Update DockerAiderExecutionStrategy for environment variable passthrough
- [x] Update SidecarAiderExecutionStrategy for environment variable handling
- [x] Ensure all strategies handle empty API keys with environment fallback
- [x] Update command generation logic to include environment variables when needed

## Backward Compatibility
- [x] Ensure existing providers with stored API keys continue to work unchanged
- [x] Test migration of existing provider configurations
- [x] Validate that all existing functionality remains intact
- [x] Add migration utilities for provider configuration updates
