# Gemini LLM Integration with Aider

## Getting Started

### Prerequisites
- You'll need a [Gemini API key](https://aistudio.google.com/app/u/2/apikey)

### Installation

1. Install aider:
```bash
python -m pip install -U aider-chat
```

2. Install Google Generative AI package:
```bash
# Option 1: Direct pip install
pip install -U google-generativeai

# Option 2: With pipx
pipx inject aider-chat google-generativeai
```

### API Key Configuration

Set your Gemini API key:
```bash
# Mac/Linux
export GEMINI_API_KEY=<your_key>

# Windows
setx GEMINI_API_KEY <your_key>
```

## Using Gemini Models

### Running Gemini Models

Run Gemini 2.5 Pro model:
```bash
aider --model gemini-2.5-pro
```

### Listing Available Models

List Gemini models:
```bash
aider --list-models gemini/
```

## Using with CodingAider

CodingAider supports Gemini models natively. You can:

1. Select a Gemini model from the dropdown in the Aider dialog
2. Configure your API key in the settings
3. Create a custom Gemini provider with specific settings

## Additional Resources
- [GitHub Repository](https://github.com/Aider-AI/aider)
- [Discord Community](https://discord.gg/Tv2uQnR88V)
