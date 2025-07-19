# MCP Server Configuration

This document describes how to configure MCP clients to use the coding-aider persistent files MCP server.

## Overview

The coding-aider plugin includes an MCP server that provides tools for managing persistent files in your project context. This server allows MCP clients (like Claude Code, Anthropic's CLI, or other MCP-compatible tools) to interact with your persistent file list.

## Server Details

- **Server Name**: `coding-aider-persistent-files`
- **Version**: `1.0.0`
- **Transport**: HTTP (runs on localhost:8080 by default)
- **Capabilities**: Tools with list change notifications

## Available Tools

### 1. `get_persistent_files`
Retrieve the current list of persistent files in the project context.

**Input Schema**: No parameters required
```json
{}
```

**Output**: List of persistent files with their properties
```json
{
  "content": [
    {
      "type": "text",
      "text": "Retrieved 3 persistent files"
    },
    {
      "type": "text", 
      "text": "Files: [{\"filePath\":\"src/main/...\", \"isReadOnly\":false, \"normalizedPath\":\"...\"}]"
    }
  ]
}
```

### 2. `add_persistent_files`
Add files to the persistent files context.

**Input Schema**:
```json
{
  "type": "object",
  "properties": {
    "files": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "filePath": {
            "type": "string"
          },
          "isReadOnly": {
            "type": "boolean",
            "default": false
          }
        },
        "required": ["filePath"]
      }
    }
  },
  "required": ["files"]
}
```

**Example Input**:
```json
{
  "files": [
    {
      "filePath": "src/main/kotlin/MyClass.kt",
      "isReadOnly": false
    },
    {
      "filePath": "README.md",
      "isReadOnly": true
    }
  ]
}
```

### 3. `remove_persistent_files`
Remove files from the persistent files context.

**Input Schema**:
```json
{
  "type": "object",
  "properties": {
    "filePaths": {
      "type": "array",
      "items": {
        "type": "string"
      }
    }
  },
  "required": ["filePaths"]
}
```

**Example Input**:
```json
{
  "filePaths": [
    "src/main/kotlin/MyClass.kt",
    "README.md"
  ]
}
```

### 4. `clear_persistent_files`
Clear all files from the persistent files context.

**Input Schema**: No parameters required
```json
{}
```

## Client Configuration

### Claude Code CLI

To configure Claude Code to use the coding-aider MCP server, add the following to your MCP configuration:

**Note**: The MCP server runs automatically when the coding-aider plugin is loaded in IntelliJ IDEA. You just need to configure Claude Code to connect to the running server.

```json
{
  "mcpServers": {
    "coding-aider-persistent-files": {
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

### Other MCP Clients

For other MCP clients, configure them to connect to the HTTP transport of the coding-aider MCP server. The exact configuration will depend on your specific MCP client implementation.

#### Generic HTTP Configuration

Most MCP clients support HTTP transport configuration similar to this:

```json
{
  "servers": {
    "coding-aider-persistent-files": {
      "transport": "http",
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

## Usage Examples

### Adding Files to Persistent Context

```bash
# Using Claude Code CLI
claude-code --mcp-server coding-aider-persistent-files call-tool add_persistent_files \
  --args '{"files": [{"filePath": "src/main/kotlin/MyClass.kt", "isReadOnly": false}]}'
```

### Listing Persistent Files

```bash
# Using Claude Code CLI
claude-code --mcp-server coding-aider-persistent-files call-tool get_persistent_files
```

### Removing Files from Context

```bash
# Using Claude Code CLI
claude-code --mcp-server coding-aider-persistent-files call-tool remove_persistent_files \
  --args '{"filePaths": ["src/main/kotlin/MyClass.kt"]}'
```

### Clearing All Persistent Files

```bash
# Using Claude Code CLI
claude-code --mcp-server coding-aider-persistent-files call-tool clear_persistent_files
```

## Development and Testing

### Running the Server Standalone

For testing purposes, you can run the MCP server standalone:

```bash
# In the coding-aider project directory
./gradlew runMcpServer
```

### Testing with MCP Inspector

You can use the MCP Inspector to test the server:

1. Start the MCP server
2. Connect the MCP Inspector to the STDIO transport
3. Test each tool to ensure proper functionality

### Debugging

To debug MCP server issues:

1. Check IntelliJ IDEA logs for server startup messages
2. Verify the persistent file service is properly initialized
3. Test each tool individually to isolate issues
4. Use MCP Inspector to validate tool schemas and responses

## Troubleshooting

### Common Issues

1. **Server not starting**: Check that the coding-aider plugin is properly installed and enabled
2. **Tool not found**: Verify the MCP client is properly configured to connect to the server
3. **Permission errors**: Ensure the MCP client has proper permissions to access the project files
4. **Connection issues**: Check that the STDIO transport is properly configured

### Logs

Server logs are available in the IntelliJ IDEA logs. Look for messages related to `McpServerService` for debugging information.

## Integration with Development Workflow

The MCP server integrates seamlessly with your development workflow:

1. **Context Management**: Use the persistent files tools to maintain context across different AI interactions
2. **Project Navigation**: Keep track of important files that need to be consistently available
3. **Collaboration**: Share persistent file contexts with team members using MCP clients
4. **Documentation**: Maintain documentation files in the persistent context for easy reference

## Security Considerations

- The MCP server only provides access to persistent file management, not direct file system access
- All file operations go through the existing persistent file service with proper validation
- The server respects the existing security model of the coding-aider plugin
- No sensitive information is exposed through the MCP interface beyond what's already available in the persistent files

## Future Enhancements

Potential future enhancements to the MCP server:

- Support for SSE (Server-Sent Events) transport
- Additional tools for project management
- Integration with version control operations
- Support for file content preview through MCP
- Batch operations for better performance