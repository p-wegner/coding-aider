[Coding Aider Plan - Checklist]

See plan: [configurable_llm_providers.md](configurable_llm_providers.md)

# Implementation Checklist

## Data Model
- [ ] Create CustomLlmProvider data class to store provider configurations
- [ ] Add provider type enum (OPENAI, OLLAMA, OPENROUTER)
- [ ] Create storage service for custom providers
- [ ] Implement secure credential storage for API keys

## UI Components
- [ ] Create "Manage Custom Providers" dialog
  - [ ] Provider type selection
  - [ ] Provider-specific input fields
  - [ ] Validation for required fields
  - [ ] Optional display name field
- [ ] Add "Manage Providers" button to settings
- [ ] Update LLM dropdown renderer to show custom providers
- [ ] Add tooltips and validation feedback

## Settings Integration  
- [ ] Extend AiderSettings to store custom provider list
- [ ] Update settings UI to show custom providers
- [ ] Add migration for existing custom OpenAI settings

## Provider Support
- [ ] Implement OpenAI provider configuration
- [ ] Add Ollama provider support
- [ ] Add OpenRouter provider support
- [ ] Create provider-specific validation rules

## Testing
- [ ] Unit tests for data model
- [ ] UI tests for provider dialog
- [ ] Integration tests for settings storage
- [ ] Validation tests for each provider type

## Documentation
- [ ] Update settings documentation
- [ ] Add provider configuration guide
- [ ] Document migration path
