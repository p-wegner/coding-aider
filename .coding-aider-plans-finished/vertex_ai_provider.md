[Coding Aider Plan]

# Add Vertex AI Support as Custom LLM Provider

## Overview

Add support for Google Cloud's Vertex AI as a new custom LLM provider type in the Coding Aider plugin, allowing users to
leverage Vertex AI's language models like Claude through the existing custom provider interface.

## Problem Description

Currently, the plugin supports OpenAI, Ollama, and OpenRouter as custom LLM providers. However, there's no support for
Google Cloud's Vertex AI platform, which offers access to various language models including Claude. Users who want to
use Vertex AI models need this integration.

## Goals

1. Add VERTEX as a new LlmProviderType enum value
2. Extend the custom provider interface to handle Vertex AI specific configuration
3. Support proper authentication via Google Cloud credentials
4. Enable model name validation for Vertex AI models
5. Integrate with existing API key management system
6. Maintain consistency with the existing provider interface

## Additional Notes and Constraints

- Requires Google Cloud authentication setup
- Need to handle project ID and location settings
- Should support both service account and user credentials
- Must maintain backward compatibility with existing providers
- Should follow the documentation patterns established in vertex.md

## References

- [Vertex AI Integration Documentation](./vertex.md)
- [Existing Custom Provider Implementation](../src/main/kotlin/de/andrena/codingaider/settings/CustomLlmProvider.kt)
- [Provider Type Definitions](../src/main/kotlin/de/andrena/codingaider/settings/LlmProviderType.kt)

See [Checklist](vertex_ai_provider_checklist.md) for implementation steps.
