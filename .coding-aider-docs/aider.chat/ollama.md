# Ollama

Aider can connect to local Ollama models.

## Instructions

### Pull the Model
```bash
ollama pull <model>
```

### Start Your Ollama Server
```bash
ollama serve
```

### In Another Terminal Window
```bash
python -m pip install -U aider-chat
export OLLAMA_API_BASE=http://127.0.0.1:11434 # Mac/Linux
setx OLLAMA_API_BASE http://127.0.0.1:11434 # Windows, restart shell after setx
aider --model ollama/<model>
```

### Example
To use the model `llama3:70b`, run:
```bash
ollama pull llama3:70b
ollama serve
```
In another terminal window:
```bash
export OLLAMA_API_BASE=http://127.0.0.1:11434 # Mac/Linux
setx OLLAMA_API_BASE http://127.0.0.1:11434 # Windows, restart shell after setx
aider --model ollama/llama3:70b
```

See the [model warnings](https://aider.chat/docs/llms/warnings.html) section for information on warnings which will occur when working with models that aider is not familiar with.
