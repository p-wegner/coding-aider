[Coding Aider Plan]

# Optional API Keys with Environment Variable Fallback for Custom LLM Providers

## Overview
Enhance the custom LLM provider system to support optional API keys with environment variable fallback capabilities. This will allow users to:
1. Create custom LLM providers without providing an API key
2. Fall back to default environment variables for that provider type when no key is provided
3. Use provider-specific or global environment variable strategies as needed

## Problem Description
Currently, the custom LLM provider system requires API keys for all providers that support authentication. However, many users want to:
- Use environment variables for API keys instead of storing them in the credential store
- Have flexible authentication where they can optionally provide an API key or rely on environment variables
- Use standard environment variable names (like OPENAI_API_KEY) for common provider types
- Support both custom provider-specific keys and global environment variable fallbacks

The current ApiKeyChecker and CustomLlmProviderEditorDialog don't properly handle the concept of "optional" API keys with environment variable fallbacks.

## Goals
1. **Enhanced Provider Configuration**: Allow custom LLM providers to be created without requiring API keys
2. **Environment Variable Fallback**: Implement fallback to default environment variables when no provider-specific key is provided
3. **Flexible Authentication Strategy**: Support both explicit API keys and environment variable-based authentication
4. **UI Improvements**: Update the provider editor dialog to reflect optional API key status with clear indicators
5. **Backward Compatibility**: Maintain existing functionality for providers with explicit API keys
6. **Enhanced Error Handling**: Provide clear feedback about API key availability and sources

## Technical Implementation Approach

### Core Architecture Changes
1. **LlmProviderType Enhancements**:
   - Add `apiKeyOptional` flag (already exists in LM_STUDIO)
   - Add default environment variable names for each provider type
   - Enhance `getApiKeyName()` to return default environment variable names

2. **CustomLlmProvider Class Updates**:
   - Add optional API key field with null/empty support
   - Add field to track whether API key was explicitly provided
   - Add validation for environment variable fallback logic

3. **ApiKeyChecker Enhancements**:
   - Modify `isApiKeyAvailableForLlm()` to handle optional API keys
   - Add method to check environment variable availability
   - Enhance `getApiKeyForLlm()` to support environment variable fallback
   - Update key retrieval logic to check: provider key → environment variable → default provider variable

4. **CustomLlmProviderEditorDialog Updates**:
   - Modify validation logic to allow empty API keys for optional providers
   - Add visual indicators for optional vs required API keys
   - Implement environment variable validation and availability checking
   - Update tooltips to explain environment variable fallback behavior

### Environment Variable Strategy
- **Provider-Specific Variables**: Use standard environment variables like OPENAI_API_KEY, ANTHROPIC_API_KEY, etc.
- **Default Provider Variables**: Use provider-type-specific defaults (e.g., OPENAI_API_KEY for OpenAI-based custom providers)
- **Custom Variables**: Allow users to specify custom environment variable names
- **Fallback Chain**: Check → provider-specific key → environment variable → default provider variable

### UI/UX Improvements
1. **Visual Indicators**: 
   - Show padlock icon for required API keys
   - Show open icon for optional API keys with environment variable fallback
   - Display key source (stored, environment, not available)

2. **Validation Feedback**:
   - Real-time environment variable availability checking
   - Clear error messages for missing required keys
   - Helpful tooltips explaining fallback behavior

3. **Documentation Integration**:
   - Inline help for each provider type's environment variable requirements
   - Examples of common environment variable configurations

## Additional Notes and Constraints

### Constraints
- **Backward Compatibility**: Existing providers with stored API keys must continue to work unchanged
- **Security**: Environment variable checking must not expose sensitive information
- **Performance**: Environment variable lookups should be cached to avoid repeated file system access
- **Cross-Platform Support**: Must work on Windows, macOS, and Linux

### Edge Cases to Handle
1. **Mixed Authentication**: Provider with explicit key should override environment variables
2. **Variable Name Conflicts**: Ensure custom provider names don't conflict with standard environment variable names
3. **Partial Configuration**: Handle cases where only some authentication parameters are provided
4. **Environment Variable Precedence**: Define clear order of precedence for key resolution

### Integration Points
- **AiderExecutionStrategy**: Must handle optional API keys in command generation
- **Docker Strategy**: Must support environment variable passthrough for authentication
- **Settings Migration**: Handle migration of existing providers to new optional format
- **Testing Strategy**: Comprehensive unit and integration tests for all authentication scenarios

## References
- Existing CustomLlmProvider implementation
- ApiKeyChecker and ApiKeyManager utilities
- LlmProviderType enum definitions
- CustomLlmProviderEditorDialog UI components
- Environment variable management in `retrieveApiKey()` method
- Docker execution strategy environment variable handling
