[Coding Aider Plan]

# Configurable LLM Providers

See checklist: [configurable_llm_providers_checklist.md](configurable_llm_providers_checklist.md)

## Overview
Add support for configuring multiple LLM providers (OpenAI, Ollama, OpenRouter) with custom endpoints, API keys and model names. This will allow users to easily add and manage their own LLM configurations through a dedicated dialog.

## Problem Description
Currently, the plugin only supports adding custom OpenAI-compatible models through environment variables or settings. This is inflexible and requires users to manually manage API keys and endpoints. Support for other providers like Ollama and OpenRouter is limited.

## Goals
1. Allow users to add/edit/remove custom LLM configurations through a dedicated dialog
2. Support different provider types (OpenAI-compatible, Ollama, OpenRouter) 
3. Store configurations securely and persistently
4. Display custom configurations in LLM selection dropdowns
5. Make the UI intuitive and user-friendly
6. Maintain backward compatibility with existing configurations

## Additional Notes and Constraints
- Custom configurations should be stored securely using IntelliJ's credential store
- The UI should guide users through provider-specific requirements
- Existing environment variables and .env files should still work
- Provider types have different requirements:
  - OpenAI: Base URL + API key + model name
  - Ollama: Base URL + model name
  - OpenRouter: API key + model name

## Execution Strategy Changes
The AiderExecutionStrategy needs to be updated to:
1. Handle different provider-specific environment variables and settings
2. Support provider-specific command line arguments
3. Manage different API key patterns for each provider
4. Handle provider-specific Docker configurations
5. Support provider-specific validation and error handling

## References
- [OpenAI Compatible APIs](openai-compat.md)
- [Ollama Integration](ollama.md) 
- [OpenRouter Integration](openrouter.md)
