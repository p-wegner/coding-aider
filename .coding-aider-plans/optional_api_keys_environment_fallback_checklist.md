[Coding Aider Plan - Checklist]

# Implementation Checklist - Optional API Keys with Environment Variable Fallback

## LlmProviderType Enhancements
- [ ] Add `apiKeyOptional` flag to all relevant provider types (OPENAI, OPENROUTER, GEMINI)
- [ ] Define default environment variable names for each provider type
- [ ] Enhance `getApiKeyName()` method to return default environment variable names
- [ ] Update provider documentation with environment variable requirements

## CustomLlmProvider Class Updates
- [ ] Add optional API key field with null/empty support
- [ ] Add field to track whether API key was explicitly provided
- [ ] Add validation for environment variable fallback logic
- [ ] Update serialization/deserialization to handle optional keys

## ApiKeyChecker Enhancements
- [ ] Modify `isApiKeyAvailableForLlm()` to handle optional API keys
- [ ] Add method to check environment variable availability specifically
- [ ] Enhance `getApiKeyForLlm()` to support environment variable fallback
- [ ] Update key retrieval logic for fallback chain: provider key → environment variable → default provider variable
- [ ] Update `getAllLlmOptions()` to reflect API key availability with environment fallbacks
- [ ] Enhance `getApiKeysForDocker()` to handle environment variable passthrough
- [ ] Update caching mechanism to work with environment variable lookups

## CustomLlmProviderEditorDialog Updates
- [ ] Modify validation logic to allow empty API keys for optional providers
- [ ] Add visual indicators (icons/colors) for optional vs required API keys
- [ ] Implement real-time environment variable validation and availability checking
- [ ] Update tooltips to explain environment variable fallback behavior
- [ ] Add environment variable name field for custom variable specification
- [ ] Update provider type selection to show API key requirements clearly
- [ ] Enhance error messages with environment variable guidance

## Environment Variable Management
- [ ] Create utility class for environment variable management
- [ ] Implement caching for environment variable lookups
- [ ] Add support for custom environment variable names
- [ ] Handle environment variable precedence and override rules
- [ ] Add validation for environment variable format and accessibility

## UI/UX Improvements
- [ ] Design and implement visual indicators for API key status
- [ ] Create tooltips and inline help for environment variable usage
- [ ] Update settings panels to show API key source (stored/environment/not available)
- [ ] Add configuration examples for common provider setups
- [ ] Implement real-time feedback for environment variable availability

## Integration with Execution Strategies
- [ ] Update NativeAiderExecutionStrategy to handle optional API keys
- [ ] Update DockerAiderExecutionStrategy for environment variable passthrough
- [ ] Update SidecarAiderExecutionStrategy for environment variable handling
- [ ] Ensure all strategies handle empty API keys with environment fallback
- [ ] Update command generation logic to include environment variables when needed

## Error Handling and Validation
- [ ] Implement comprehensive validation for environment variable availability
- [ ] Add clear error messages for missing required keys
- [ ] Handle edge cases like malformed environment variables
- [ ] Add validation for environment variable name conflicts
- [ ] Implement fallback when environment variables are unavailable

## Testing and Documentation
- [ ] Create comprehensive unit tests for all API key scenarios
- [ ] Add integration tests for environment variable fallback functionality
- [ ] Test cross-platform environment variable handling
- [ ] Add documentation for new optional API key features
- [ ] Create user guides for environment variable configuration
- [ ] Update existing documentation to reflect new API key options

## Backward Compatibility
- [ ] Ensure existing providers with stored API keys continue to work unchanged
- [ ] Test migration of existing provider configurations
- [ ] Validate that all existing functionality remains intact
- [ ] Add migration utilities for provider configuration updates

## Performance Optimization
- [ ] Implement caching for environment variable lookups
- [ ] Optimize real-time validation to avoid performance impact
- [ ] Add lazy loading for environment variable checks
- [ ] Ensure minimal overhead for providers without API keys
