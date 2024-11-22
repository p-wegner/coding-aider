[Coding Aider Plan]

# Add Custom Model Support to Plugin Settings

## Overview
Add support for configuring custom OpenAI-compatible models in the Coding Aider plugin settings, allowing users to leverage Aider's compatibility with various LLM providers through custom API endpoints.

## Problem Description
Currently, the plugin lacks direct UI support for configuring custom OpenAI-compatible models, which Aider supports. Users need to configure these settings through environment variables or configuration files outside the IDE.

## Goals
1. Add UI elements to configure custom model settings:
    - Custom API base URL input
    - Custom API key input
    - Custom model name input with "openai/" prefix validation
2. Integrate these settings with existing LLM provider configuration
3. Persist custom model settings
4. Validate custom model settings
5. Apply custom model settings to Aider commands

## Additional Notes and Constraints
- Custom models must be prefixed with "openai/" as per Aider's requirements
- Need to handle secure storage of API keys
- Must validate API base URL format
- Should provide clear feedback for invalid configurations
- Should maintain backward compatibility with existing settings

## References
- [Checklist](add_custom_model_support_checklist.md)
- [Context](add_custom_model_support_context.yaml)
- [Aider OpenAI Compatible APIs Documentation](https://aider.chat/docs/llms/openai-compat.html)