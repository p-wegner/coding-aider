[Coding Aider Plan]

# Custom LLM Providers Without API Keys

## Overview
Add support for custom LLM providers that don't require API key configuration, extending the existing custom provider system. This will allow users to specify custom model names that will be used directly with the --model parameter in AiderExecutionStrategy.

## Problem Description
Currently, the custom LLM provider system is tightly coupled with API key management. However, some use cases require only model name customization without API key handling. This is particularly useful for:
- Local model deployments
- Custom model naming schemes
- Alternative model providers with different authentication methods

## Goals
1. Allow creation of custom LLM providers without API key requirements
2. Maintain compatibility with existing provider system
3. Ensure proper model name handling in AiderExecutionStrategy
4. Update UI to reflect API key optional status
5. Provide clear documentation for users

## Additional Notes and Constraints
- Must preserve existing API key functionality for providers that need it
- Should integrate seamlessly with current UI
- Need to handle model name prefixing appropriately
- Should maintain clear separation between providers requiring and not requiring API keys

## References
- Existing CustomLlmProvider implementation
- AiderExecutionStrategy model parameter handling
- Settings UI for provider management
