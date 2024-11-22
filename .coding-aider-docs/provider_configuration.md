# LLM Provider Configuration Guide

This guide explains how to set up and use different LLM providers with the Coding Aider plugin.

## OpenAI and Compatible Providers

### Standard OpenAI
1. Get your API key from OpenAI
2. Add it in Settings > Coding Aider > API Keys
3. Select an OpenAI model from the dropdown

### Custom OpenAI-compatible
1. Click "Manage Custom Providers"
2. Select "OpenAI Compatible"
3. Enter:
   - Display Name (e.g., "Local GPT4All")
   - Base URL (e.g., "http://localhost:8080/v1")
   - API Key
   - Model Name

## Ollama Integration

### Setup
1. Install Ollama on your system
2. Start the Ollama service
3. In Coding Aider settings:
   - Click "Manage Custom Providers"
   - Select "Ollama"
   - Enter:
     - Display Name
     - Base URL (default: http://localhost:11434)
     - Model Name (e.g., "llama2")

### Usage Tips
- Ensure Ollama is running before using
- Check Ollama logs if connection fails
- Model must be pulled in Ollama before use

## OpenRouter Integration

### Setup
1. Get API key from OpenRouter
2. In Coding Aider settings:
   - Click "Manage Custom Providers"
   - Select "OpenRouter"
   - Enter:
     - Display Name
     - API Key
     - Model Name

### Available Models
- See OpenRouter's website for current model list
- Popular options:
  - anthropic/claude-2
  - openai/gpt-4-turbo
  - google/palm-2
