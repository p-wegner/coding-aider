# OpenRouter Documentation

Aider can connect to [models provided by OpenRouter](https://openrouter.ai/models?o=top-weekly). You’ll need an [OpenRouter API key](https://openrouter.ai/keys).

## Installation

To install Aider, run the following command:

```bash
python -m pip install -U aider-chat
```

### Setting Up Your API Key

For Mac/Linux:
```bash
export OPENROUTER_API_KEY=<key>
```

For Windows:
```bash
setx OPENROUTER_API_KEY <key> # Restart shell after setx
```

## Using OpenRouter Models

To use a specific model, run:
```bash
aider --model openrouter/<provider>/<model>
```

### Listing Available Models

To list models available from OpenRouter, use:
```bash
aider --list-models openrouter/
```

## Troubleshooting

If you encounter errors, check your [OpenRouter privacy settings](https://openrouter.ai/settings/privacy). Ensure to “enable providers that may train on inputs” to allow use of all models.
