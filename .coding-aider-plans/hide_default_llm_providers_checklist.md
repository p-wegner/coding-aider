[Coding Aider Plan - Checklist]

# Hide Default LLM Providers Implementation Checklist

## Data Model Changes

- [x] Add hidden state storage for default providers
- [x] Create persistence mechanism for hidden state
- [x] Update DefaultApiKeyChecker to track hidden providers

## UI Updates

- [x] Modify CustomLlmProviderDialog to handle default provider visibility
- [x] Update provider list display to show hidden state for default providers
- [x] Enable hide/show button for default providers
- [x] Update button states based on provider type

## Service Layer

- [ ] Add methods to toggle default provider visibility
- [ ] Implement visibility check for default providers
- [ ] Update provider filtering logic

Related: [Main Plan](hide_default_llm_providers.md)
