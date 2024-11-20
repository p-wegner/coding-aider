# OpenAI Compatible APIs

Aider can connect to any LLM which is accessible via an OpenAI compatible API endpoint.

## Installation

To install Aider, run the following command:

```bash
python -m pip install -U aider-chat
```

## Configuration

### Mac/Linux

Set the following environment variables:

```bash
export OPENAI_API_BASE=<endpoint>
export OPENAI_API_KEY=<key>
```

### Windows

Set the following environment variables:

```bash
setx OPENAI_API_BASE <endpoint>
setx OPENAI_API_KEY <key>
```

**Note:** Restart the shell after using `setx` commands.

## Usage

Prefix the model name with `openai/` when using Aider:

```bash
aider --model openai/<model-name>
```

For more information on model warnings, refer to the [model warnings](https://aider.chat/docs/llms/warnings.html) section.