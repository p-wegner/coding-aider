[Coding Aider Plan]

# Hide Default LLM Providers

## Overview
Add functionality to hide default LLM providers similar to how custom providers can be hidden, giving users more control over which LLM options appear in their interface.

## Problem Description
Currently, default LLM providers defined in DefaultApiKeyChecker are always visible in the UI. Users cannot hide these providers even if they don't use them, leading to a potentially cluttered interface. While custom providers can be hidden, this functionality is not available for default providers.

## Goals
- Allow users to hide/show default LLM providers
- Maintain consistency with existing custom provider hiding mechanism
- Preserve hidden state across IDE sessions
- Update UI to reflect hidden status of default providers
- Ensure backwards compatibility

## Additional Notes and Constraints
- Must preserve existing functionality for custom providers
- Need to store hidden state persistently
- Should integrate seamlessly with existing UI
- Must maintain clear distinction between built-in and custom providers
- Should not affect API key management functionality

## References
- [Checklist](hide_default_llm_providers_checklist.md)
- Related files listed in [Context](hide_default_llm_providers_context.yaml)
