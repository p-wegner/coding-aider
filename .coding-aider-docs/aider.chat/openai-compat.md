# OpenAI Compatible APIs

Aider can connect to any LLM which is accessible via an OpenAI compatible API endpoint.

## Installation

To install Aider, run the following command:

```bash
python -m pip install -U aider-chat
```

### Mac/Linux:

```bash
export OPENAI_API_BASE=<endpoint>
export OPENAI_API_KEY=<key>
```

### Windows:

```bash
setx OPENAI_API_BASE <endpoint>
setx OPENAI_API_KEY <key>
```

**Note:** Restart the shell after using `setx` commands.

## Usage

Prefix the model name with `openai/`:

```bash
aider --model openai/<model-name>
```

See the [model warnings](https://aider.chat/docs/llms/warnings.html) section for information on warnings which will occur when working with models that Aider is not familiar with.
