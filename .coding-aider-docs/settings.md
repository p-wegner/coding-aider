# Settings Documentation

## LLM Provider Configuration

The plugin supports multiple LLM providers that can be configured through the settings:

### Built-in Providers
- OpenAI (default)
- Anthropic Claude
- DeepSeek

### Custom Provider Support
You can add custom providers through the "Manage Custom Providers" dialog:

1. OpenAI-compatible:
   - Requires: Base URL, API Key, Model Name
   - Useful for self-hosted models or alternative OpenAI-compatible endpoints

2. Ollama:
   - Requires: Base URL, Model Name
   - No API key needed
   - Perfect for local model deployment

3. OpenRouter:
   - Requires: API Key, Model Name
   - Provides access to multiple model providers through one interface

### API Key Management
- API keys are stored securely in IntelliJ's credential store
- Keys can be provided through:
  - Settings UI
  - Environment variables
  - .env files

### Configuration Tips
- Test your configuration using the "Test Connection" button
- Use meaningful display names for custom providers
- Ensure proper base URLs for Ollama and OpenAI-compatible providers
