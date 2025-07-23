# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Common Development Commands

### Build and Test
```bash
# Build the plugin
./gradlew build

# Run tests
./gradlew test

# Build and run IntelliJ IDEA with the plugin
./gradlew runIde

# Build plugin distribution
./gradlew buildPlugin

# Verify plugin compatibility
./gradlew verifyPlugin
```

### Testing
- **Unit Tests**: Located in `src/test/kotlin` - use JUnit 5 and AssertJ
- **Integration Tests**: Extend `BaseIntegrationTest` for service integration testing
- **Test Command**: `./gradlew test` runs all tests with JUnit Platform
- Run individual test: `./gradlew test --tests "ClassName.methodName"`

### Development Workflow
- **IDE Development**: Use `./gradlew runIde` to launch test IntelliJ instance
- **Plugin Installation**: Built plugin is in `build/distributions/`
- **Logging**: Set log level via system property in `runIde` task (default: INFO)

## Architecture Overview

### Core Module Structure
The plugin follows a layered architecture with clear separation of concerns:

1. **Actions Layer** (`actions/`): User-initiated commands and IDE integrations
   - `aider/`: Core Aider integration actions (AiderAction, DocumentCodeAction, etc.)
   - `git/`: Git-related operations (CommitAction, GitCodeReviewAction)
   - `ide/`: IDE-specific utilities (PersistentFilesAction, SettingsAction)

2. **Services Layer** (`services/`): Business logic and state management
   - **Core Services**: AiderHistoryService, PersistentFileService, TokenCountService
   - **Plan Management**: AiderPlanService, ContinuePlanService (structured mode)
   - **MCP Integration**: McpServerService with tools for persistent file management
   - **Execution**: CommandSummaryService, RunningCommandService

3. **Executors Layer** (`executors/`): Command execution strategies
   - **CommandExecutor**: Main execution interface
   - **Strategies**: NativeAiderExecutionStrategy, DockerAiderExecutionStrategy, SidecarAiderExecutionStrategy
   - **Observers**: CommandObserver for progress tracking

4. **Input/Output Layer**: User interaction and result presentation
   - **InputDialog** (`inputdialog/`): AiderInputDialog with context management
   - **OutputView** (`outputview/`): Markdown rendering and result display

### Key Design Patterns

**Command Pattern**: CommandData encapsulates execution context with file lists, working directory, and execution preferences.

**Strategy Pattern**: Multiple execution strategies (Native, Docker, Sidecar) implementing AiderExecutionStrategy interface.

**Observer Pattern**: Command execution progress tracked via CommandObserver with lifecycle callbacks (onCommandStarted, onCommandFinished, etc.).

**Service Locator**: IntelliJ service architecture used throughout with application and project-level services.

## Aider Integration Modes

### Execution Strategies
1. **Native**: Direct aider command execution (requires aider in PATH)
2. **Docker**: Containerized execution using `paulgauthier/aider` image
3. **Sidecar**: Persistent aider process for improved performance

### Input Modes
- **IDE-based**: Execution within IntelliJ with integrated output
- **Shell-based**: Terminal execution with file context pre-setup

### Context Management
- **Persistent Files**: Managed via PersistentFileService, stored in project settings
- **File Context**: Automatic inclusion of selected files and persistent files
- **Working Directory**: Configurable root directory for aider operations

## Structured Mode (Plan-Based Development)

### Plan System Architecture
Plans are stored in `.coding-aider-plans/` with three file types per plan:
- `{plan_name}.md`: Main plan with overview and goals
- `{plan_name}_checklist.md`: Granular implementation tasks
- `{plan_name}_context.yaml`: Associated implementation files

### Plan Services
- **AiderPlanService**: Plan CRUD operations and file management
- **AiderPlanPromptService**: Plan generation and refinement prompts
- **ActivePlanService**: Current plan state tracking
- **ContinuePlanService**: Plan continuation and progress tracking

### Plan Workflow
1. Enable structured mode in AiderInputDialog
2. Plans auto-created with context files
3. Checklist items marked complete during implementation
4. Tool window provides plan overview and management

## MCP Server Integration

The plugin includes an HTTP MCP server (localhost:8080) providing tools for persistent file management:

### Available Tools
- `get_persistent_files`: Retrieve current persistent file list
- `add_persistent_files`: Add files to persistent context
- `remove_persistent_files`: Remove files from context
- `clear_persistent_files`: Clear all persistent files

### MCP Service Architecture
- **McpServerService**: HTTP server lifecycle management
- **McpTool interface**: Tool implementation contract
- **McpToolRegistry**: Tool discovery and registration
- Tools located in `services/mcp/tools/`

## Settings and Configuration

### Settings Hierarchy
- **Application Level**: AiderSettings (global aider path, docker config, LLM providers)
- **Project Level**: AiderProjectSettings (persistent files, working directory, plan preferences)

### LLM Provider Support
- Built-in providers: OpenAI, Anthropic, Ollama, OpenRouter, Gemini, Vertex AI
- Custom provider configuration via CustomLlmProviderService
- Provider-specific settings (API keys, base URLs, model names)

### Key Configuration Areas
- **Execution**: Strategy selection (native/docker/sidecar)
- **Docker**: Container management and volume mounting
- **Plans**: Structured mode preferences and templates
- **UI**: Dialog behavior, keyboard shortcuts, output formatting

## Important Implementation Notes

### Action System
- Actions extend AnAction with ActionUpdateThread.BGT for performance
- Keyboard shortcuts defined in plugin.xml
- Context-aware action visibility via update() method
- Action groups provide hierarchical menu organization

### File Processing
- FileData class encapsulates file metadata (path, read-only status)
- File normalization handles cross-platform path differences
- Directory traversal utilities in FileTraversal class
- Git integration via GitUtils for change detection

### Error Handling
- Intention actions for compile error fixing (FixCompileErrorAction)
- TODO detection and resolution (FixTodoAction)
- Error parsing and context extraction for AI assistance

### Threading Model
- Background tasks for long-running operations (web crawl, plan generation)
- EDT-safe UI updates via ApplicationManager.getApplication().invokeLater()
- Reactive programming with reactor-core for async operations

## Docker Integration

### DockerContainerManager
- Container lifecycle management (start, stop, remove)
- Volume mounting for project file access
- Image management and updates

### Docker Execution Strategy
- Automatic image pulling and container setup
- File system mounting with proper permissions
- Command execution within containerized environment

## Development Best Practices

### Code Organization
- Package structure mirrors functional domains
- Companion objects for static utilities and factories
- Extension functions for IntelliJ platform integration
- Nullable types used appropriately with Kotlin idioms

### Testing Strategy
- Unit tests for isolated components (services, utilities)
- Integration tests for service interactions
- Mock-based testing with Mockito for external dependencies
- Test data located in `src/test/resources/`

### Plugin Integration
- IntelliJ platform services registered in plugin.xml
- Tool windows for persistent UI components
- Notification groups for user feedback
- Intention actions for contextual code assistance