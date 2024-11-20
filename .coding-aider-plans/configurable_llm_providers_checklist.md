[Coding Aider Plan - Checklist]

See plan: [configurable_llm_providers.md](configurable_llm_providers.md)

# Implementation Checklist

## Data Model
- [x] Create CustomLlmProvider data class to store provider configurations
- [x] Add provider type enum (OPENAI, OLLAMA, OPENROUTER)
- [x] Create storage service for custom providers
- [x] Implement secure credential storage for API keys

## UI Components
- [x] Create "Manage Custom Providers" dialog
  - [x] Provider type selection
  - [x] Provider-specific input fields
  - [x] Validation for required fields
  - [x] Optional display name field
- [x] Add "Manage Providers" button to settings
- [x] Update LLM dropdown renderer to show custom providers
- [x] Add tooltips and validation feedback

## Settings Integration  
- [ ] Extend AiderSettings to store custom provider list
- [ ] Update settings UI to show custom providers
- [ ] Add migration for existing custom OpenAI settings

## Provider Support
- [x] Implement OpenAI provider configuration
- [x] Add Ollama provider support
- [x] Add OpenRouter provider support
- [x] Create provider-specific validation rules

## Execution Strategy Implementation
- [ ] Create provider-specific environment variable handlers
- [ ] Implement provider-specific command line argument builders
- [ ] Add Docker configuration support for each provider
- [ ] Implement provider-specific API key management
- [ ] Add validation and error handling per provider

## Documentation
- [ ] Update settings documentation
- [ ] Add provider configuration guide
