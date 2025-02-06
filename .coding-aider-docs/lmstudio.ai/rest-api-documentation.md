# LM Studio REST API (beta)

**Requires [LM Studio 0.3.6](https://lmstudio.ai/download) or newer. Still WIP, endpoints may change.**

LM Studio now has its own REST API, in addition to OpenAI compatibility mode ([learn more](https://lmstudio.ai/docs/api/openai-api)). The REST API includes enhanced stats such as Token / Second and Time To First Token (TTFT), as well as rich information about models such as loaded vs unloaded, max context, quantization, and more.

## Supported API Endpoints

- **GET /api/v0/models** - List available models
- **GET /api/v0/models/{model}** - Get info about a specific model
- **POST /api/v0/chat/completions** - Chat Completions (messages -> assistant response)
- **POST /api/v0/completions** - Text Completions (prompt -> completion)
- **POST /api/v0/embeddings** - Text Embeddings (text -> embedding)

### Example Requests

#### GET /api/v0/models
```bash
curl http://localhost:1234/api/v0/models
```

#### GET /api/v0/models/{model}
```bash
curl http://localhost:1234/api/v0/models/qwen2-vl-7b-instruct
```

#### POST /api/v0/chat/completions
```bash
curl http://localhost:1234/api/v0/chat/completions \
-H "Content-Type: application/json" \
-d '{
  "model": "granite-3.0-2b-instruct",
  "messages": [
    { "role": "system", "content": "Always answer in rhymes." },
    { "role": "user", "content": "Introduce yourself." }
  ],
  "temperature": 0.7,
  "max_tokens": -1,
  "stream": false
}'
```

#### POST /api/v0/completions
```bash
curl http://localhost:1234/api/v0/completions \
-H "Content-Type: application/json" \
-d '{
  "model": "granite-3.0-2b-instruct",
  "prompt": "the meaning of life is",
  "temperature": 0.7,
  "max_tokens": 10,
  "stream": false,
  "stop": "\n"
}'
```

#### POST /api/v0/embeddings
```bash
curl http://127.0.0.1:1234/api/v0/embeddings \
-H "Content-Type: application/json" \
-d '{
  "model": "text-embedding-nomic-embed-text-v1.5",
  "input": "Some text to embed"
}'
```

### Example Responses

#### Response for GET /api/v0/models
```json
{
  "object": "list",
  "data": [
    {
      "id": "qwen2-vl-7b-instruct",
      "object": "model",
      "type": "vlm",
      "publisher": "mlx-community",
      "arch": "qwen2_vl",
      "compatibility_type": "mlx",
      "quantization": "4bit",
      "state": "not-loaded",
      "max_context_length": 32768
    }
  ]
}
```

#### Response for POST /api/v0/chat/completions
```json
{
  "id": "chatcmpl-i3gkjwthhw96whukek9tz",
  "object": "chat.completion",
  "created": 1731990317,
  "model": "granite-3.0-2b-instruct",
  "choices": [
    {
      "index": 0,
      "finish_reason": "stop",
      "message": {
        "role": "assistant",
        "content": "Greetings, I'm a helpful AI, here to assist,\nIn providing answers, with no distress.\nI'll keep it short and sweet, in rhyme you'll find,\nA friendly companion, all day long you'll bind."
      }
    }
  ],
  "usage": {
    "prompt_tokens": 24,
    "completion_tokens": 53,
    "total_tokens": 77
  }
}
```

### Feedback
🚧 We are in the process of developing this interface. Let us know what's important to you on [GitHub](https://github.com/lmstudio-ai/lmstudio.js/issues) or by [email](mailto:example@example.com).