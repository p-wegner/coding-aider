# AGENTS.md – Coding-Aider IntelliJ Plugin

## Build & Test

```powershell
./gradlew build          # full build
./gradlew test           # all tests (JUnit 5)
./gradlew runIde         # launch IntelliJ sandbox with plugin loaded
./gradlew buildPlugin    # produces build/distributions/
./gradlew test --tests "de.andrena.codingaider.services.plans.AiderPlanServiceTest.someMethod"
```

- **JVM target**: 21; **Kotlin**: 2.1.10; **Platform**: IntelliJ IC 2025.2 (`sinceBuild = 242`)
- Plugin version is read from `version.txt` at build time.

## Package Layout

All production code lives under `de.andrena.codingaider`:

| Package | Responsibility |
|---|---|
| `actions/aider`, `git`, `ide` | `AnAction` entry-points; collect files, open dialogs, dispatch to executors |
| `command/` | `CommandData` – the single transfer object through the whole call chain |
| `executors/api/` | `IDEBasedExecutor`, `ShellExecutor`, `SimpleExecutor`; observe `CommandObserver` lifecycle |
| `executors/strategies/` | `AiderExecutionStrategy` (abstract) → `NativeAiderExecutionStrategy`, `DockerAiderExecutionStrategy`, `SidecarAiderExecutionStrategy` |
| `services/plans/` | Plan CRUD, prompt generation, subplan tracking |
| `services/mcp/` | Ktor/SSE HTTP server (default port 8080); `McpTool` interface + `McpToolRegistry` |
| `services/sidecar/` | Persistent aider process management |
| `features/` | `customactions/`, `documentation/`, `testgeneration/`, `webcrawl/` |
| `settings/` | `AiderSettings` (APP-level) + `AiderProjectSettings` (PROJECT-level) |

## Command Execution Flow

```
AnAction.actionPerformed
  → FileDataCollectionService.collectAllFiles()
  → AiderInputDialog (user configures mode/model/files)
  → CommandData (immutable DTO)
  → IDEBasedExecutor / ShellExecutor
      → CommandExecutor.executeCommand()
          → AiderExecutionStrategy.buildCommand()   ← Native | Docker | Sidecar
          → Process + ProcessOutputReader
          → CommandObserver callbacks (onCommandStart / onCommandProgress / onCommandComplete)
```

`CommandData.options.useDockerAider` overrides the global setting; `null` means "use setting".

## Key Design Patterns

- **Observer**: `CommandSubject` / `CommandObserver` – `IDEBasedExecutor` registers as observer; notify via `notifyObservers { it.onCommandComplete(...) }`.
- **Strategy**: All three execution strategies implement `buildCommand()` + `prepareEnvironment()` + `cleanupAfterExecution()`. `CommandExecutor` picks the strategy lazily.
- **Service Locator**: IntelliJ DI – use `project.service<AiderPlanService>()` (project-level) or `getApplication().getService(CustomLlmProviderService::class.java)` (app-level).
- **Actions**: Always override `getActionUpdateThread()` → `ActionUpdateThread.BGT`. Use `update()` to guard visibility.

## Aider Modes (`AiderMode` enum)

`NORMAL` | `ARCHITECT` | `STRUCTURED` | `SHELL` – set on `CommandData.aiderMode`.  
Structured mode creates/reads plans in `.coding-aider-plans/`; `CommandData.structuredMode` is a convenience getter.

## Plan File Conventions

Plans reside in `.coding-aider-plans/` (configurable via `AiderProjectSettings.plansFolderPath`):

| File | Marker in first line |
|---|---|
| `{name}.md` | `[Coding Aider Plan]` |
| `{name}_checklist.md` | `[Coding Aider Plan - Checklist]` |
| `{name}_context.yaml` | YAML with `files:` list |

Single-file format merges all three sections with `---` separators.  
`AiderPlanService` constants: `AIDER_PLAN_MARKER`, `AIDER_PLAN_CHECKLIST_MARKER`, `AIDER_PLANS_FOLDER`.

## LLM Model Argument Handling

In `AiderExecutionStrategy.buildCommonArgs()`:
- Model string starting with `--` → passed directly as CLI flag.
- Otherwise → `--model <name>`.
- Custom providers resolved via `CustomLlmProviderService.getProvider(llm)` → prefix applied only when `requiresModelPrefix == true` and not already prefixed.

## Settings

- `AiderSettings` (app): docker image, sidecar mode, edit format, auto-commits, LLM providers — stored in `AiderSettings.xml` (roaming disabled).
- `AiderProjectSettings` (project): working directory, plans folder, test/doc/custom-action type configurations — stored in `AiderProjectSettings.xml`.
- Access: `AiderSettings.getInstance()` / `AiderProjectSettings.getInstance(project)`.

## Testing Conventions

- IntelliJ-dependent tests extend `BasePlatformTestCase` and are `@Disabled` by default (require the IntelliJ test framework to be set up at runtime).
- Pure unit tests use **Mockito-Kotlin** + **AssertJ** directly, no base class needed.
- `ServiceContainerUtil` (in test sources) provides helpers for registering mock services in the platform container.
- Test resources: `src/test/resources/plans/`, `src/test/resources/testdata/`.

## MCP Server

- `McpServerService` starts a Ktor CIO HTTP/SSE server; port auto-selected from 8080 upward.
- Add tools by implementing `McpTool` and registering in `McpToolRegistry` (auto-discovered via `McpToolMetadata`).
- Enabled/disabled via `AiderSettings.enableMcpServer`; tool window availability updated on settings change.

## Key External Libraries

| Library | Used for |
|---|---|
| `flexmark` | Markdown parsing & rendering in output view |
| `jtokkit` | Token counting for context estimation |
| `reactor-core` | Async/reactive flows (web crawl, chain commands) |
| `io.modelcontextprotocol:kotlin-sdk` | MCP server protocol |
| `Ktor CIO` | HTTP server for MCP |
| `jackson-dataformat-yaml` | Context YAML parsing |

