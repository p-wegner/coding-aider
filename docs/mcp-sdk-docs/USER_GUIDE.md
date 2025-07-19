# MCP Kotlin SDK User Guide

## Table of Contents
- [Introduction](#introduction)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Creating MCP Clients](#creating-mcp-clients)
- [Creating MCP Servers](#creating-mcp-servers)
- [Transport Layers](#transport-layers)
- [Configuration Options](#configuration-options)
- [Sample Projects](#sample-projects)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

## Introduction

The MCP Kotlin SDK is a multiplatform implementation of the [Model Context Protocol](https://modelcontextprotocol.io) (MCP) that enables applications to provide context for Large Language Models (LLMs) in a standardized way. This SDK supports both client and server implementations across JVM, WebAssembly, and iOS platforms.

### Key Features

- **Multiplatform Support**: Works on JVM, WebAssembly (WASM), and iOS
- **Multiple Transport Layers**: STDIO, Server-Sent Events (SSE), and WebSocket
- **Complete MCP Implementation**: Full support for resources, prompts, tools, and notifications
- **Type-Safe API**: Leverages Kotlin's type system for safe MCP message handling
- **Coroutine Support**: Built with Kotlin coroutines for asynchronous operations

### Use Cases

- Build MCP clients that connect to any MCP server
- Create MCP servers that expose resources, prompts, and tools to LLMs
- Integrate with AI applications and LLM surfaces
- Provide standardized context to language models

## Installation

Add the MCP Kotlin SDK to your project:

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.modelcontextprotocol:kotlin-sdk:0.6.0")
}
```

### Gradle (Groovy DSL)

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'io.modelcontextprotocol:kotlin-sdk:0.6.0'
}
```

### Maven

```xml
<dependency>
    <groupId>io.modelcontextprotocol</groupId>
    <artifactId>kotlin-sdk</artifactId>
    <version>0.6.0</version>
</dependency>
```

## Quick Start

### Basic Client Example

```kotlin
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.Implementation

suspend fun main() {
    val client = Client(
        clientInfo = Implementation(
            name = "my-mcp-client",
            version = "1.0.0"
        )
    )
    
    // Connect using STDIO transport
    val transport = StdioClientTransport(
        input = processInputStream,
        output = processOutputStream
    )
    
    client.connect(transport)
    
    // List available tools
    val tools = client.listTools()
    println("Available tools: ${tools?.tools?.map { it.name }}")
    
    client.close()
}
```

### Basic Server Example

```kotlin
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.*

suspend fun main() {
    val server = Server(
        serverInfo = Implementation(
            name = "my-mcp-server",
            version = "1.0.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true)
            )
        )
    )
    
    // Add a simple tool
    server.addTool(
        name = "hello",
        description = "Says hello",
        inputSchema = Tool.Input()
    ) { request ->
        CallToolResult(
            content = listOf(TextContent("Hello, World!"))
        )
    }
    
    // Connect using STDIO transport
    val transport = StdioServerTransport()
    server.connect(transport)
}
```

## Creating MCP Clients

### Client Configuration

```kotlin
val client = Client(
    clientInfo = Implementation(
        name = "your-client-name",
        version = "1.0.0"
    ),
    options = ClientOptions(
        // Optional configuration
    )
)
```

### Common Client Operations

#### Listing Resources

```kotlin
val resources = client.listResources()
resources?.resources?.forEach { resource ->
    println("Resource: ${resource.name} - ${resource.uri}")
}
```

#### Reading Resources

```kotlin
val content = client.readResource(
    ReadResourceRequest(uri = "file:///example.txt")
)
content?.contents?.forEach { resourceContent ->
    when (resourceContent) {
        is TextResourceContents -> println(resourceContent.text)
        is BlobResourceContents -> println("Binary content: ${resourceContent.blob.size} bytes")
    }
}
```

#### Calling Tools

```kotlin
val result = client.callTool(
    name = "weather_forecast",
    arguments = mapOf(
        "latitude" to 37.7749,
        "longitude" to -122.4194
    )
)

result?.content?.forEach { content ->
    when (content) {
        is TextContent -> println(content.text)
        is ImageContent -> println("Image: ${content.data}")
    }
}
```

#### Getting Prompts

```kotlin
val prompt = client.getPrompt(
    GetPromptRequest(
        name = "code_review",
        arguments = mapOf("language" to "kotlin")
    )
)

prompt?.messages?.forEach { message ->
    println("${message.role}: ${message.content}")
}
```

### Advanced Client Features

#### Handling Notifications

```kotlin
client.onNotification { notification ->
    when (notification) {
        is ResourceUpdatedNotification -> {
            println("Resource updated: ${notification.uri}")
        }
        is ToolListChangedNotification -> {
            println("Tool list changed")
        }
    }
}
```

#### Progress Tracking

```kotlin
client.onProgress { progress ->
    println("Progress: ${progress.progress}/${progress.total}")
}
```

## Creating MCP Servers

### Server Configuration

```kotlin
val server = Server(
    serverInfo = Implementation(
        name = "your-server-name",
        version = "1.0.0"
    ),
    options = ServerOptions(
        capabilities = ServerCapabilities(
            prompts = ServerCapabilities.Prompts(listChanged = true),
            resources = ServerCapabilities.Resources(
                subscribe = true,
                listChanged = true
            ),
            tools = ServerCapabilities.Tools(listChanged = true)
        ),
        enforceStrictCapabilities = true
    )
)
```

### Adding Tools

#### Simple Tool

```kotlin
server.addTool(
    name = "calculator",
    description = "Performs basic arithmetic operations",
    inputSchema = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("operation") {
                put("type", "string")
                put("enum", JsonArray(listOf(
                    JsonPrimitive("add"),
                    JsonPrimitive("subtract"),
                    JsonPrimitive("multiply"),
                    JsonPrimitive("divide")
                )))
            }
            putJsonObject("a") {
                put("type", "number")
            }
            putJsonObject("b") {
                put("type", "number")
            }
        },
        required = listOf("operation", "a", "b")
    )
) { request ->
    val operation = request.arguments["operation"]?.jsonPrimitive?.content
    val a = request.arguments["a"]?.jsonPrimitive?.doubleOrNull
    val b = request.arguments["b"]?.jsonPrimitive?.doubleOrNull
    
    if (operation == null || a == null || b == null) {
        return@addTool CallToolResult.error("Missing required parameters")
    }
    
    val result = when (operation) {
        "add" -> a + b
        "subtract" -> a - b
        "multiply" -> a * b
        "divide" -> if (b != 0.0) a / b else return@addTool CallToolResult.error("Division by zero")
        else -> return@addTool CallToolResult.error("Unknown operation")
    }
    
    CallToolResult(
        content = listOf(TextContent("Result: $result"))
    )
}
```

#### Tool with Error Handling

```kotlin
server.addTool(
    name = "file_reader",
    description = "Reads content from a file",
    inputSchema = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("path") {
                put("type", "string")
                put("description", "Path to the file to read")
            }
        },
        required = listOf("path")
    )
) { request ->
    try {
        val path = request.arguments["path"]?.jsonPrimitive?.content
            ?: return@addTool CallToolResult.error("Path parameter is required")
        
        val content = File(path).readText()
        CallToolResult(
            content = listOf(TextContent(content))
        )
    } catch (e: Exception) {
        CallToolResult.error("Failed to read file: ${e.message}")
    }
}
```

### Adding Resources

```kotlin
server.addResource(
    uri = "file:///config.json",
    name = "Application Configuration",
    description = "Main application configuration file",
    mimeType = "application/json"
) { request ->
    val configContent = """
        {
            "app_name": "My Application",
            "version": "1.0.0",
            "debug": false
        }
    """.trimIndent()
    
    ReadResourceResult(
        contents = listOf(
            TextResourceContents(
                text = configContent,
                uri = request.uri,
                mimeType = "application/json"
            )
        )
    )
}
```

### Adding Prompts

```kotlin
server.addPrompt(
    name = "code_review",
    description = "Generate a code review for the given code",
    arguments = listOf(
        PromptArgument(
            name = "language",
            description = "Programming language of the code",
            required = true
        ),
        PromptArgument(
            name = "code",
            description = "Code to review",
            required = true
        )
    )
) { request ->
    val language = request.arguments?.get("language") ?: "unknown"
    val code = request.arguments?.get("code") ?: ""
    
    GetPromptResult(
        description = "Code review for $language code",
        messages = listOf(
            PromptMessage(
                role = Role.user,
                content = TextContent(
                    "Please review this $language code and provide feedback:\n\n$code"
                )
            )
        )
    )
}
```

### Server Lifecycle Management

```kotlin
server.onClose {
    println("Server is closing...")
    // Cleanup resources
}

server.onError { error ->
    println("Server error: ${error.message}")
}
```

## Transport Layers

### STDIO Transport

STDIO transport is ideal for command-line applications and process-based communication.

#### Client STDIO

```kotlin
val transport = StdioClientTransport(
    input = process.inputStream.asSource().buffered(),
    output = process.outputStream.asSink().buffered()
)
```

#### Server STDIO

```kotlin
val transport = StdioServerTransport(
    inputStream = System.`in`.asSource().buffered(),
    outputStream = System.out.asSink().buffered()
)
```

### Server-Sent Events (SSE) Transport

SSE transport is perfect for web-based applications and real-time communication.

#### Using Ktor Plugin (Recommended)

```kotlin
import io.ktor.server.application.*
import io.modelcontextprotocol.kotlin.sdk.server.mcp

fun Application.module() {
    mcp {
        Server(
            serverInfo = Implementation(
                name = "sse-server",
                version = "1.0.0"
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = true)
                )
            )
        )
    }
}
```

#### Manual SSE Configuration

```kotlin
suspend fun runSseServer(port: Int) {
    embeddedServer(CIO, host = "0.0.0.0", port = port) {
        install(SSE)
        routing {
            sse("/sse") {
                val transport = SseServerTransport("/message", this)
                val server = configureServer()
                
                server.connect(transport)
            }
            post("/message") {
                // Handle POST messages
                val sessionId = call.request.queryParameters["sessionId"]!!
                // Process message...
            }
        }
    }.start(wait = true)
}
```

### WebSocket Transport

WebSocket transport provides full-duplex communication for real-time applications.

#### Client WebSocket

```kotlin
val client = HttpClient()
val transport = WebSocketClientTransport(
    client = client,
    urlString = "ws://localhost:8080/mcp"
)
```

#### Server WebSocket

```kotlin
routing {
    webSocket("/mcp") {
        val transport = WebSocketMcpServerTransport(this)
        val server = configureServer()
        server.connect(transport)
    }
}
```

## Configuration Options

### Client Options

```kotlin
data class ClientOptions(
    val capabilities: ClientCapabilities? = null,
    val enforceStrictCapabilities: Boolean = true
)
```

### Server Options

```kotlin
data class ServerOptions(
    val capabilities: ServerCapabilities,
    val enforceStrictCapabilities: Boolean = true
)
```

### Server Capabilities

```kotlin
ServerCapabilities(
    prompts = ServerCapabilities.Prompts(
        listChanged = true  // Server can notify about prompt list changes
    ),
    resources = ServerCapabilities.Resources(
        subscribe = true,   // Clients can subscribe to resource updates
        listChanged = true  // Server can notify about resource list changes
    ),
    tools = ServerCapabilities.Tools(
        listChanged = true  // Server can notify about tool list changes
    ),
    logging = ServerCapabilities.Logging(), // Enable logging support
    experimental = mapOf(
        "customFeature" to JsonObject(mapOf(
            "enabled" to JsonPrimitive(true)
        ))
    )
)
```

## Sample Projects

The SDK includes three comprehensive sample projects:

### 1. Weather STDIO Server

A weather information server that provides forecast and alert tools using the National Weather Service API.

**Location**: `samples/weather-stdio-server/`

**Features**:
- Weather forecast by latitude/longitude
- Weather alerts by US state
- STDIO transport
- HTTP client integration with Ktor

**Running**:
```bash
cd samples/weather-stdio-server
./gradlew build
java -jar build/libs/weather-stdio-server-0.1.0-all.jar
```

### 2. Kotlin MCP Server

A multiplatform server demonstrating various server configurations and transport methods.

**Location**: `samples/kotlin-mcp-server/`

**Features**:
- Multiplatform support (JVM, WASM)
- Multiple transport options (STDIO, SSE)
- Sample tools, prompts, and resources
- Ktor integration

**Running**:
```bash
cd samples/kotlin-mcp-server
./gradlew runJvm                    # JVM with SSE
./gradlew wasmJsNodeDevelopmentRun  # WASM with Node.js
```

### 3. Kotlin MCP Client

An interactive client that connects to MCP servers and integrates with Anthropic's API.

**Location**: `samples/kotlin-mcp-client/`

**Features**:
- STDIO transport client
- Anthropic API integration
- Interactive chat loop
- Tool execution and response handling

**Running**:
```bash
cd samples/kotlin-mcp-client
./gradlew build
java -jar build/libs/kotlin-mcp-client-0.1.0-all.jar path/to/server.jar
```

## Best Practices

### Error Handling

Always implement proper error handling in your tools and resources:

```kotlin
server.addTool(
    name = "safe_operation",
    description = "A tool with proper error handling",
    inputSchema = Tool.Input()
) { request ->
    try {
        // Your tool logic here
        CallToolResult(
            content = listOf(TextContent("Success"))
        )
    } catch (e: Exception) {
        CallToolResult.error("Operation failed: ${e.message}")
    }
}
```

### Resource Management

Use `use` blocks or proper cleanup for resources:

```kotlin
client.use { mcpClient ->
    // Use the client
    val result = mcpClient.listTools()
    // Client will be automatically closed
}
```

### Async Operations

Leverage Kotlin coroutines for non-blocking operations:

```kotlin
server.addTool(
    name = "async_operation",
    description = "An asynchronous operation",
    inputSchema = Tool.Input()
) { request ->
    // This is already running in a coroutine context
    val result = withContext(Dispatchers.IO) {
        // Perform I/O operation
        performNetworkCall()
    }
    
    CallToolResult(
        content = listOf(TextContent(result))
    )
}
```

### Type Safety

Use sealed classes and when expressions for type-safe message handling:

```kotlin
client.onNotification { notification ->
    when (notification) {
        is ResourceUpdatedNotification -> handleResourceUpdate(notification)
        is ToolListChangedNotification -> handleToolListChange(notification)
        is PromptListChangedNotification -> handlePromptListChange(notification)
        else -> println("Unknown notification: $notification")
    }
}
```

### Configuration Management

Externalize configuration for flexibility:

```kotlin
data class ServerConfig(
    val name: String,
    val version: String,
    val port: Int,
    val enableLogging: Boolean
)

fun createServer(config: ServerConfig): Server {
    return Server(
        serverInfo = Implementation(
            name = config.name,
            version = config.version
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                logging = if (config.enableLogging) {
                    ServerCapabilities.Logging()
                } else null
            )
        )
    )
}
```

## Troubleshooting

### Common Issues

#### Connection Problems

**Issue**: Client cannot connect to server
**Solution**: 
- Verify the server is running and accessible
- Check transport configuration (ports, paths)
- Ensure proper protocol version compatibility

#### Serialization Errors

**Issue**: JSON serialization/deserialization failures
**Solution**:
- Verify input schema matches the actual arguments
- Check for required vs optional fields
- Use proper JSON types (JsonPrimitive, JsonObject, JsonArray)

#### Tool Execution Failures

**Issue**: Tools return errors or unexpected results
**Solution**:
- Add comprehensive error handling
- Validate input parameters
- Use CallToolResult.error() for error responses

### Debugging Tips

1. **Enable Logging**: Add SLF4J implementation for detailed logs
2. **Use MCP Inspector**: Connect to `http://localhost:port/sse` for SSE servers
3. **Validate Schemas**: Ensure tool input schemas match expected arguments
4. **Check Capabilities**: Verify server capabilities match client expectations
### Performance Considerations

1. **Connection Pooling**: Reuse HTTP clients for multiple requests
2. **Async Operations**: Use coroutines for I/O-bound operations
3. **Resource Cleanup**: Always close clients and servers properly
4. **Memory Management**: Be mindful of large resource contents

For more detailed information, refer to the [MCP Specification](https://spec.modelcontextprotocol.io/) and the [official documentation](https://modelcontextprotocol.io/introduction).
