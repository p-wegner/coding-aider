# Chat Modes in Aider

Aider supports several chat modes for different interaction purposes:

- `code` - Makes direct changes to your code based on requests
- `architect` - First proposes a solution, then asks permission to implement changes
- `ask` - Answers questions about your code without making edits
- `help` - Provides guidance about using aider, configuration, and troubleshooting

## Using Chat Modes

By default, aider starts in "code" mode. You can:

1. Use single-message mode commands:
   - `/code` 
   - `/architect`
   - `/ask`
   - `/help`

2. Switch the active mode permanently:
   ```
   /chat-mode code
   /chat-mode architect
   /chat-mode ask
   /chat-mode help
   ```

3. Launch aider in a specific mode:
   - Use `--chat-mode <mode>`
   - Or use `--architect` shortcut for architect mode

## Architect Mode Details

Architect mode uses two models sequentially:

1. Main model (configured via `/model` or `--model`):
   - Analyzes your request
   - Proposes a solution
   - Asks for approval before making changes

2. Editor model:
   - Handles the actual code editing
   - Can be specified with `--editor-model <model>`
   - Uses default settings if not specified

### Benefits of Architect Mode

- Produces better results than code mode
- Particularly useful with OpenAI's o1 models
- Works well when pairing different models (e.g., o1 for architecture, GPT-4 for editing)
- Even beneficial when using the same model for both roles
- Allows two-step problem solving for better results

Note: Architect mode uses two LLM requests, making it potentially slower and more expensive than code mode.

For more details on edit formats and configuration, see [aider's architect/editor mode](https://aider.chat/2024/09/26/architect.html).
