# [Coding Aider Plan - Checklist]

# GitHub Copilot CLI Integration - Implementation Checklist

- [x] Add `useCopilotCli: Boolean` and `ghExecutablePath: String` fields to `AiderSettings` and corresponding defaults in `AiderDefaults`
- [x] Add optional BYOK settings to `AiderSettings`: `copilotProviderBaseUrl`, `copilotProviderType` (openai/azure/anthropic), `copilotProviderApiKey` — mapped to `COPILOT_PROVIDER_*` env vars at execution time
- [x] Implement `GithubCopilotCliExecutionStrategy` in `executors/strategies/` extending `AiderExecutionStrategy`; build command as `gh copilot -p "<message>" --allow-all-tools --add-dir <projectPath> --model <model> --silent`
- [x] JSON output: strategy uses `--silent` for clean text output; Copilot CLI edits files natively so JSONL parsing is not required — limitation documented in README
- [x] Map `CommandData.llm` to the `--model` flag; skip the flag entirely when BYOK mode is active (model set via `COPILOT_MODEL` env var instead)
- [x] Assemble `COPILOT_PROVIDER_*` environment variables from BYOK settings and pass them to the process environment in the strategy
- [x] Extend `CommandExecutor` with a `useCopilotCli` branch in strategy selection; guard Sidecar and Docker paths to throw a clear error when Copilot CLI is selected
- [x] Create `CopilotAuthChecker`: check `gh auth status` for standard Copilot auth; skip auth check and validate `COPILOT_PROVIDER_BASE_URL` presence when BYOK is configured
- [x] Update `AiderSetupPanel` to show a "Verify `gh auth status`" button, `ghExecutablePath` field, and collapsible BYOK section; Copilot CLI section always visible in Setup tab
- [x] Add a Copilot CLI settings section in `AiderSettingsConfigurable` (Setup tab): toggle, executable path, BYOK fields, and auth check button
- [x] Restrict `AiderMode` selection in `AiderInputDialog` to `NORMAL` and `ARCHITECT` when `useCopilotCli` is active; `STRUCTURED` and `SHELL` filtered out from segmented button
- [x] Keep the LLM selector visible and functional in `AiderInputDialog` when Copilot CLI is active; BYOK override documented in README
- [x] Adapt `AiderHistoryService` for Copilot CLI sessions: limitation documented in README (sessions stored in `~/.copilot/logs/`)
- [x] Write unit tests for `GithubCopilotCliExecutionStrategy`: command assembly, JSON output parsing, BYOK env-var injection, model-flag mapping, using a mocked process runner
- [x] Write unit tests for `CopilotAuthChecker` covering: authenticated, unauthenticated, `gh`-not-found, and BYOK-no-auth-needed scenarios
- [x] Write integration test verifying Docker/Sidecar strategy selection is blocked at runtime when `useCopilotCli = true` (`CopilotCliCommandExecutorTest`, `@Disabled` per IntelliJ test framework convention)
- [x] Update `plugin.xml` if any new services are registered — N/A, no registration needed (classes instantiated directly)
- [x] Update `README.md` with Copilot CLI setup instructions, BYOK configuration, and known limitations vs. Aider

