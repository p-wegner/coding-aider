# Settings Documentation

## LLM Provider Configuration

The plugin supports multiple LLM providers that can be configured through the settings:

### Built-in Providers
- OpenAI (default)
- Anthropic Claude
- DeepSeek
- Vertex AI (Google Cloud)

### Custom Provider Support
You can add custom providers through the "Manage Custom Providers" dialog:

1. OpenAI-compatible:
   - Requires: Base URL, API Key, Model Name
   - Useful for self-hosted models or alternative OpenAI-compatible endpoints
   - Model names can be prefixed with "openai/" or left unprefixed

2. Ollama:
   - Requires: Base URL, Model Name
   - No API key needed - uses local authentication
   - Perfect for local model deployment
   - Model names can be prefixed with "ollama/" or left unprefixed

3. OpenRouter:
   - Requires: API Key, Model Name
   - Provides access to multiple model providers through one interface
   - Model names should include provider prefix (e.g., "anthropic/claude-3")

4. Vertex AI:
   - Requires: Google Cloud Authentication
   - No API key needed - uses Google Cloud credentials
   - Supports Gemini and other Google AI models
   - Model names should include @latest suffix (e.g., "gemini-pro@latest")

5. LM Studio:
   - Requires: Base URL (default: http://localhost:1234/v1)
   - Optional: API Key for secured instances
   - Perfect for local model deployment
   - Uses OpenAI-compatible API endpoints
   - Model names can be prefixed with "lm_studio/" or left unprefixed
   - Environment Variables:
     - LMSTUDIO_API_BASE: Base URL for LM Studio instance
     - LMSTUDIO_API_KEY: Optional API key if authentication is enabled

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
