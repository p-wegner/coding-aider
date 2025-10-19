## Bug Fixes
- [ ] Bug: (sidecar) verify env vars (api keys) for sidecar mode properly set
- [ ] Bug: Properly handle /ask with SystemPrompt for input history parsing
- [ ] Bug: plan cost calculation fails to detect model properly
- [ ] Bug: archive plan does not move history file
- [ ] Bug: abort command when closing tab in output tool window

## Plan & Workflow Enhancements
- [ ] count first plan execution in history
- [ ] Feature: adjust context yaml when moving files
- [ ] Feature: plan view
  - [ ] add tests (using existing test creation feature)
- [ ] plan style: - minimal, - medium, - full (custom prd template?)
  - [x] minimal: single plan file
- [ ] Improve Subplan Feature to properly run subplans when executing the master plan

## Testing & Quality Automation
- [ ] Improve: Generate Tests: make context file easier to setup
- [ ] Improve: Generate Tests: which files to include in context? persistent files? currently persistent files are not used for context
- [ ] Feature: generate/adjust tests for last changes
- [ ] Feature: TDD mode with configurable test type
- [ ] Feature: add tests for changes

## UI & UX Enhancements
- [ ] Feature: Paste Image in aider dialog should call the clipboard image action
- [ ] feature: follow up command suggestion and execution (user confirmation)
- [ ] Show some settings in tool coding aider tool window
  - [ ] main llm
- [ ] Feature: context view file ending options
- [ ] Feature: scratch file support with aider subtree only (scratch files need to be moved?)
- [ ] Feature: extend todo tool context menu with fix action (multiselect)
- [ ] Feature: give clipboard images a name when pasting (optional)

## Context & Persistent File Management
- [ ] Feature: rename file should update persistent files (if used)
- [ ] Feature: Consistent Model and API Key Management
  - [ ] Streamline custom aider model/ api key setup
- [ ] allow using env vars in provider config (for custom providers)

## Execution & Integration Enhancements
- [ ] (Experimental) Feature: provide some mcp server functionality
  - server: plan creation, crud for persistent files, repo map, 
  - client: interpret output and call mcps and shell commands, start mcp servers
- [ ] aiderless toolcalling/mcp mode
- [ ] support different agentic clis: claude code, gemini, codex, (generic adapter setting or only limited set? usually arguments will differ and model selection is not possible for most of them)

## Settings & Refactoring
- [ ] refactor: group aider specific settings

## Before Release
