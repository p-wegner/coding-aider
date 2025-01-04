[Coding Aider Plan - Checklist]

# Implementation Checklist

## Provider System Updates
- [ ] Update LlmProviderType to support API key optional providers
- [ ] Modify CustomLlmProvider to handle API key optional status
- [ ] Update model name handling in CustomLlmProvider

## UI Changes
- [ ] Update CustomLlmProviderDialog to handle API key optional providers
- [ ] Modify provider editor dialog UI based on provider type
- [ ] Update validation logic for provider creation/editing

## Command Generation
- [ ] Update AiderExecutionStrategy to handle API key optional providers
- [ ] Ensure proper model name formatting in command generation

## Documentation
- [ ] Update settings documentation with API key optional providers
- [ ] Add examples for different provider configurations

## Testing
- [ ] Test provider creation without API keys
- [ ] Verify command generation with custom providers
- [ ] Test UI behavior with different provider types
