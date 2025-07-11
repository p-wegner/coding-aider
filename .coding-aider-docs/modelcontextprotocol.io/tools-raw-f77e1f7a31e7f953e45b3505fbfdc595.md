# Model Context Protocol – Tools

**Protocol Revision:** 2025-06-18  
**Home:** https://modelcontextprotocol.io/

The Model Context Protocol (MCP) allows servers to expose _tools_ that language models can discover and invoke. Each tool has a unique name and metadata describing its schema.

---

## Capabilities

Servers that support tools **MUST** declare the `tools` capability:

```json
{
  "capabilities": {
    "tools": { "listChanged": true }
  }
}
```
- `listChanged`: whether the server sends notifications when available tools change.

---

## Listing Tools

Clients discover tools by sending a `tools/list` request (supports pagination):

**Request:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/list",
  "params": { "cursor": "optional-cursor-value" }
}
```

**Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "tools": [
      {
        "name": "get_weather",
        "title": "Weather Information Provider",
        "description": "Get current weather information for a location",
        "inputSchema": {
          "type": "object",
          "properties": {
            "location": {
              "type": "string",
              "description": "City name or zip code"
            }
          },
          "required": ["location"]
        }
      }
    ],
    "nextCursor": "next-page-cursor"
  }
}
```

---

## Calling Tools

Invoke a tool with a `tools/call` request:

**Request:**
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/call",
  "params": {
    "name": "get_weather",
    "arguments": { "location": "New York" }
  }
}
```

**Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "Current weather in New York:\nTemperature: 72°F\nConditions: Partly cloudy"
      }
    ],
    "isError": false
  }
}
```

### List-Changed Notification

If `listChanged` is enabled, the server **SHOULD** send:
```json
{
  "jsonrpc": "2.0",
  "method": "notifications/tools/list_changed"
}
```

---

## Data Types

### Tool Definition

- **name**: Unique identifier
- **title**: Optional human-readable name
- **description**: Functionality description
- **inputSchema**: JSON Schema for parameters
- **outputSchema**: (Optional) JSON Schema for outputs
- **annotations**: (Optional) Metadata

### Tool Result

Results may include:

- **Unstructured content** (`content` array):
    - **Text**:
      ```json
      { "type": "text", "text": "Tool result text" }
      ```
    - **Image**:
      ```json
      { "type": "image", "data": "base64-data", "mimeType": "image/png" }
      ```
    - **Audio**:
      ```json
      { "type": "audio", "data": "base64-audio", "mimeType": "audio/wav" }
      ```
    - **Resource link**:
      ```json
      {
        "type": "resource_link",
        "uri": "file:///project/src/main.rs",
        "name": "main.rs",
        "description": "Primary entry point",
        "mimeType": "text/x-rust"
      }
      ```
    - **Embedded resource**:
      ```json
      {
        "type": "resource",
        "resource": {
          "uri": "file:///project/src/main.rs",
          "title": "Project Rust Main File",
          "mimeType": "text/x-rust",
          "text": "fn main() {...}"
        }
      }
      ```

- **Structured content** (`structuredContent` object):
    - Serialized JSON for strict schema validation.

---

## Error Handling

1. **Protocol errors** – JSON-RPC standard errors (e.g., code `-32602`).
2. **Tool execution errors** – indicated by `"isError": true` in the result.

---

## Security Considerations

- **Servers MUST**:
    - Validate all tool inputs
    - Implement access controls and rate limits
    - Sanitize outputs

- **Clients SHOULD**:
    - Prompt user confirmation for sensitive calls
    - Display inputs before invocation
    - Validate results against schemas
    - Implement timeouts and logging  
