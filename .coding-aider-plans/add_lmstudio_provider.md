[Coding Aider Plan]

# Add LMStudio Provider Support

## Overview
Add support for LMStudio as a new LlmProviderType to enable users to connect to local LMStudio instances for AI model interactions.

## Problem Description
Currently, the application supports various LLM providers like OpenAI, Ollama, and Vertex AI, but lacks native support for LMStudio. LMStudio provides a local solution for running AI models, similar to Ollama, but requires specific configuration and handling.

## Goals
1. Add LMStudio as a new LlmProviderType
2. Implement proper configuration handling for LMStudio connections
3. Ensure compatibility with existing provider infrastructure
4. Update documentation to include LMStudio setup instructions

## Implementation Details
- Add new LMStudio enum to LlmProviderType
- Configure LMStudio similar to OpenAI-compatible providers
- Set appropriate defaults for base URL and authentication
- Update relevant documentation

## Additional Notes and Constraints
- LMStudio uses OpenAI-compatible API endpoints
- Requires base URL configuration
- No API key is required for local instances
- Should follow similar pattern to Ollama implementation
- Need to maintain backward compatibility

## References
- [LMStudio Documentation](.coding-aider-docs/aider.chat/aider.lm-studio.md)
- [Ollama Integration Example](.coding-aider-docs/aider.chat/ollama.md)
