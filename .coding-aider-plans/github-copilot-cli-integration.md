# [Coding Aider Plan]

# GitHub Copilot CLI Integration

## Overview
Integrate the GitHub Copilot CLI (`gh copilot`) as an alternative backend execution strategy alongside the existing Aider-based strategies. The CLI (v2.89.0+) is a **full agentic coding assistant** — it can edit files directly, run shell commands, select models, resume sessions, output structured JSON, and supports BYOK (Bring Your Own Key) for OpenAI, Azure, Anthropic, and Ollama.

## Problem Description
The plugin currently supports three execution strategies (Native, Docker, Sidecar) all targeting the `aider` CLI. The GitHub Copilot CLI (`gh copilot`, v2.89.0+) is now a capable agentic alternative with comparable feature depth:

| Capability | Aider | `gh copilot` CLI (v2.89.0+) |
|---|---|---|
| Direct file editing | ✅ SEARCH/REPLACE | ✅ native (`--allow-tool=write`) |
| File context | ✅ `--file` args | ✅ `--add-dir`, reads from CWD |
| Model selection | ✅ `--model` | ✅ `--model`, plus BYOK env vars |
| Multi-turn / sessions | ✅ sidecar protocol | ✅ `--continue` / `--resume` |
| Structured output | ✅ diff blocks | ✅ `--output-format json` |
| Non-interactive scripting | ✅ | ✅ `-p "prompt" --allow-all-tools --silent` |
| Custom instructions | ✅ `.aider.conf.yml` | ✅ `AGENTS.md` (loaded by default) |
| Auth | API key env vars | `gh auth login` |
| History | `.aider.chat.history.md` | `~/.copilot/logs/` + `/share` |

The core execution approach is: invoke `gh copilot -p "<message>" --allow-all-tools --add-dir <projectPath> --model <model> --output-format json --silent`.

## Goals
1. Implement `GithubCopilotCliExecutionStrategy` in `executors/strategies/` using `gh copilot -p` in non-interactive mode; pass file context via `--add-dir`, model via `--model`, and parse output with `--output-format json --silent`.
2. Extend `CommandExecutor` with a `useCopilotCli` strategy-selection branch; disable Sidecar and Docker code paths when Copilot CLI is selected.
3. Add `ghExecutablePath` and `useCopilotCli` settings to `AiderSettings` / `AiderDefaults`; surface them in `AiderSetupPanel` and the settings configurable.
4. Create a `CopilotAuthChecker` to verify GitHub auth via `gh auth status`; show a "Verify `gh auth status`" button in the setup panel instead of API-key fields when Copilot CLI is active.
5. Map `CommandData.llm` to the `--model` flag; support BYOK by exposing the `COPILOT_PROVIDER_*` environment variables (base URL, type, API key) as optional settings — this makes `CustomLlmProviderService` partially reusable for Copilot BYOK providers.
6. Restrict `AiderMode` in `AiderInputDialog` to `NORMAL` and `ARCHITECT` (autopilot-style) when Copilot CLI is active; disable `STRUCTURED` (plan-file workflow incompatible) and `SHELL` (terminal passthrough not applicable).
7. Handle history: `AiderHistoryService` writes `.aider.chat.history.md`; for Copilot CLI sessions, use `--share` to export the session to a markdown file in the project, or skip history integration entirely and rely on `~/.copilot/logs/`.

## Additional Notes and Constraints
- **Plugin-based edits NOT required**: the CLI edits files natively; do not prepend `PLUGIN_BASED_EDITS_INSTRUCTION`.
- **Permissions**: use `--allow-all-tools --allow-all-paths` for maximum compatibility; expose as a configurable option (default: allow all within project dir only via `--add-dir`).
- **`gh` CLI prerequisite**: `ghExecutablePath` defaults to `gh` on PATH; the copilot binary is auto-downloaded to `~/.local/share/github-copilot-cli/` on first use.
- **Sidecar/Docker incompatibility**: explicitly disable (UI + runtime guard) when `useCopilotCli = true`.
- **BYOK**: `COPILOT_PROVIDER_BASE_URL` activates BYOK mode; GitHub auth not required in that case — `CopilotAuthChecker` must handle both scenarios.
- **Session resumption**: `--resume=<sessionId>` enables multi-turn continuations; consider exposing this in the UI for follow-up commands.
- **ACP server mode**: `--acp` starts an Agent Client Protocol server; this could enable richer future IDE integration but is out of scope for the initial implementation.
- **JSON output parsing**: `--output-format json` emits JSONL; the strategy must parse this to extract the final response and any file-change events.
- **Testing**: mock the `gh` process; test JSON output parsing, auth-check branching, BYOK env-var assembly, and model-flag mapping.
- **Parallel operation**: all Aider strategies remain fully functional; Copilot CLI is additive.

## References
- `executors/strategies/AiderExecutionStrategy.kt` – base strategy contract
- `executors/strategies/NativeAiderExecutionStrategy.kt` – reference implementation
- `executors/CommandExecutor.kt` – strategy selection logic
- `settings/AiderSettings.kt`, `AiderDefaults.kt` – settings model
- `settings/AiderSetupPanel.kt` – setup/auth UI``
- `settings/CustomLlmProviderService.kt`, `LlmProviderType.kt` – model/provider logic (reusable for BYOK)
- `inputdialog/AiderMode.kt`, `inputdialog/AiderInputDialog.kt` – mode restrictions
- `utils/ApiKeyChecker.kt` – authentication abstraction
- `services/AiderHistoryService.kt` – history handling to adapt
- GitHub Copilot CLI docs: https://docs.github.com/copilot/how-tos/copilot-cli
- BYOK providers reference: `gh copilot -- help providers`
