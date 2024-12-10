[Coding Aider Plan - Checklist]
# Vertex AI Provider Implementation Checklist

See [Plan](./vertex_ai_provider.md) for full feature description.

## Setup
- [x] Add VERTEX enum value to LlmProviderType
- [x] Define required configuration fields (project ID, location)
- [x] Update example models and documentation

## Provider Configuration
- [x] Add Vertex AI specific fields to CustomLlmProvider
- [x] Extend CustomLlmProviderEditorDialog for Vertex settings
- [x] Implement validation for Vertex AI configuration

## Authentication
- [x] Add support for Google Cloud credentials
- [x] Integrate with existing ApiKeyManager
- [x] Handle both service account and user credentials

## UI Updates
- [x] Add Vertex AI specific input fields
- [x] Update provider type dropdown
- [x] Add tooltips and validation messages

## Testing
- [ ] Test provider creation
- [ ] Test authentication flow
- [ ] Test model name validation
- [ ] Test configuration persistence

## Documentation
- [ ] Update user documentation
- [ ] Add setup instructions
- [ ] Document supported models
