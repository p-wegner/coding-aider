- [ ] Feature: Paste Image in aider dialog should call the clipboard image action
- [ ] Bug: (sidecar) verify env vars (api keys) for sidecar mode properly set
- [ ] Bug: Properly handle /ask with SystemPrompt for input history parsing
- [ ] count first plan execution in history
- [ ] Bug: plan cost calculation fails to detect model properly
- [ ] Bug: archive plan does not move history file
- [ ] Feature: adjust context yaml when moving files
- [ ] Improve: Generate Tests: make context file easier to setup
- [ ] Improve: Generate Tests: which files to include in context? persistent files? currently persistent files
  are not used for context
- [ ] (Experimental) Feature: provide some mcp server functionality
  - server: plan creation, crud for persistent files, repo map, 
  - client: interpret output and call mcps and shell commands, start mcp servers
- [ ] aiderless toolcalling/mcp mode
- [ ] refactor: group aider specific settings 
- [ ] Feature: generate/adjust tests for last changes
- [ ] Feature: TDD mode with configurable test type
- [ ] feature: follow up command suggestion and execution (user confirmation)
- [] Feature: plan view
  - [ ] add tests (using existing test creation feature)
- [ ] Show some settings in tool coding aider tool window
  - [ ] main llm
- [ ] Bug: abort command when closing tab in output tool window
- [ ] Feature: add tests for changes
- [ ] Feature: context view file ending options
- [ ] Feature: scratch file support with aider subtree only (scratch files need to be moved?)
- [ ] Feature: extend todo tool context menu with fix action (multiselect)
- [ ] Feature: give clipboard images a name when pasting (optional)
- [ ] Feature: rename file should update persistent files (if used)
- [ ] Feature: Consistent Model and API Key Management
  - [ ] Streamline custom aider model/ api key setup
- [ ] support different agentic clis: claude code, gemini, codex, (generic adapter setting or only limited set? usually arguments will differ and model selection is not possible for most of them)
- [ ] allow using env vars in provider config (for custom providers)

## Prio Features
- [x] Feature: scratchpad feature for clipboard content and or temporary files outside aider working directory
    (reuse intellij scratchpad feature)
- [ ] plan style: - minimal, - medium, - full (custom prd template?)
  - [x] minimal: single plan file
- [x] automatic yaml file inclusion as setting

- [ ] Improve Subplan Feature to properly run subplans when executing the master plan

## Before Release

- [x] bug: fix copy paste with keyboard shortcuts in markdown viewer
- [x] Bug: aiderignore not used for every action
- [x] Bug: automatic plan continuation fails to detect plan end properly
- [x] Bug: Continue Plan doesn't properly register when plan ended
- [x] Bug: "Error executing Aider command: null" when aborting command
    - [x] in case of command abortion, make sure to keep output content of the command visible.