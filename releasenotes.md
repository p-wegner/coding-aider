# Coding-Aider Plugin Release Notes - v1.2.10

## 🌟 Key Highlights

- **Create Plan from previous command**: Create a plan from the last command result
- **Experimental Plugin-Based Edits**: Advanced code editing with flexible edit formats
- **Copy Context to clipboard**: Copy context to clipboard for easy pasting into other applications
- **Stash for persistent files**: Stash for persistent files for quick context switching
- **Enhanced Clipboard Functionality**: Apply Code edits from clipboard
- **Git Code Review Enhancements**: More flexible code review options

## 🚀 New Features

- Create Plan from previous command:
  - New action in Aider output dialog to create a plan from the last command result
  - Can be used to create plans after a command has run
  - useful if a request turns out to be too complex for single shot execution

- Experimental Plugin-Based Edits:
  - Instead of relying on aider to apply edits, the plugin can now apply them directly
  - Support for multiple edit formats (diff, whole, udiff)
  - Lenient edit processing across different formats
  - May be used in future releases to
    - foundational work for workflows and features that don't rely on aider
      - e.g. llm decides on edit format
    - fix aider limitations related to llms failing to conform to edit format (e.g. quadruple fenced ```` blocks when triple fenced ``` is part of the edit)
  - Auto-Commit Functionality (does not work reliably yet)

- Clipboard Enhancements:
  - New action to apply code edits from clipboard
  - may be used to work around limitations of aiders edit format handling
  - i.e. aider has issues creating markdown plan files => chat history can be used to extract and apply code edits
  - Support for multiple edit formats in clipboard

- Stash for Persistent Files:
  - Persistent files can now be stashed for quick context switching

- Copy Context to clipboard
  - Button in Aider input dialog to copy context to clipboard
  - Can be used to paste context into other applications
  - helpful for expensive reasoning models like o3 in subscription based chat interfaces instead of paying for tokens

- Git Code Review:
  - Ability to review changes directly from Git log
    - New Git Log Code Review action in git log context menu when 2 commits are selected
  - Flexible ref selection (branches, tags, commits)

## 💡 Improvements
## General
- Restructured prompt augmentation settings
- Proper handling of aiders slash commands in combination with plugin based prompt augmentation (e.g. structured output, plan mode)

## 🔧 Bug Fixes
- Enhanced compatibility with different LLM providers (e.g. fixed incorrect api key usage)
- Resolved issues with persistent file management

## 🛠 Technical Improvements
- Updated Kotlin version to 2.1.10
- Compatibility with IntelliJ IDEA 2025.1


# Coding-Aider Plugin Release Notes - v1.2.9

## 🌟 Key Highlights

- **Test Generation Feature**: AI-powered test creation with configurable test types
- **.aiderignore Support**: Easily exclude files from Aider context
- **Gemini Model Support**: Integration with Google's Gemini models
- **Enhanced Documentation**: Improved documentation organization and accessibility

## 🚀 New Features

- Test Generation:
  - Configurable test types with customizable templates
  - Reference file pattern matching
  - Context-aware test generation
  - Support for multiple testing frameworks
  - Test type management in project settings

- .aiderignore Support:
  - GitIgnore-style pattern matching
  - Directory exclusion with trailing slash
  - Recursive pattern matching with **
  - File extension filtering
  - Toolbar integration for quick access

- Gemini Model Integration:
  - Support for Gemini 2.5 Pro and other Gemini models
  - API key management
  - Docker container compatibility



## 💡 Improvements

- UI Enhancements:
  - Improved markdown rendering with better styling
  - Enhanced code block display
  - Better XML block formatting for summaries and intentions
  - Copy context to clipboard functionality

- Performance & Reliability:
  - Better history parsing and display
  - Improved file path handling and normalization
  - More robust pattern matching for ignored files
  - Updated to Aider v0.79.1 compatibility
- Other:
  - properly use reasoning effort setting for models that support it
  - rename base llms to aider-compatible names
  - 
## 🔧 Bug Fixes
- Enhanced file path handling in persistent files
- Fixed duplicate file detection
- Fixed wrong API key name for custom openAI providers being used
# Coding-Aider Plugin Release Notes - v1.2.8

## 🌟 Key Highlights

- **LM Studio Integration**: Full support for LM Studio instances with  LM Studio API endpoints
- **Git Code Review**: New feature for AI-powered code review between branches/commits
- **Enhanced TODO Management**: Quick and interactive TODO fix actions

## 🚀 New Features

- LM Studio Provider Support:
  - LM Studio API endpoint support
  - Optional API key support for secured instances

- Git Code Review:
  - Branch and commit comparison
  - will use persistent files, git diff and current files that were changed between refs as context
  - free-form prompt to allow different use cases, e.g. code review feedback or release note generation

- Working Directory Enhancements:
  - Action in project view to directly set working directory to a selected folder 
  - Input and Chat History parsing features will use git root as base path (aider uses git root, even when started in subdir)

- TODO Management:
  - Quick fix action for TODOs
  - Interactive mode with custom prompts
  - File context awareness
  - Integration with existing inspection system
- Reintroduce option for default AiderMode


## 💡 Improvements

- UI Enhancements:
  - Collapsible command panels in output (currently only used for logged command)
  - Dialog positioning on same screen as IDE
  - Some styling adjustments for the output viewer

- Performance & Reliability:
  - Option to enable Local model cost mapping (prevents http requests on aider startup)
  - Reasoning effort settings for built-in reasoning models 
  - Better error handling in Git operations

## 🔧 Bug Fixes
- Improved file path handling in persistent files

## Previous Versions
# Coding-Aider Plugin Release Notes - v1.2.7

## 🌟 Key Highlight

Enhanced Plan Mode stands out as the most significant feature, introducing:
- Hierarchical plan management with parent-child relationships
- Visual tree representation for better plan organization
- Improved plan continuation and refinement capabilities
- Archive system for completed plans
  This feature dramatically improves project organization and execution workflow.

## 🚀 New Features

- Working Directory Support:
  - Configure specific working directory for Aider operations
  - Files outside working directory are excluded from context
  - Working directory panel in tool window
- Improved Markdown Output Viewer (Running Command and Last Command Result):
  - Replaced Swing Browser with Chromium based JCEF Browser for better css support
  - Improved scrolling behavior and layout responsiveness
  - Better styling for code blocks and search/replace sections
  - Autoscrolling with manual scroll position saving
  - Improved Parsing for Last Command Result
- Enhanced Plan Mode:
  - Support for hierarchical plans with parent-child relationships
  - Tree visualization (experimental) for plan hierarchy
  - Reworked system prompt
  - Plan refinement capabilities
  - Improved plan continuation logic
  - Archive functionality for completed plans
- Experimental Sidecar Mode for plan execution
  - Ideally an aider process can be used until the plan is finished
- Setting for structured xml output blocks for summary and intention
- Support for custom "aider model", e.g. simple way to add aider supported llms like `r1` to dropdown without additional api key setup
- Added action to add files from a (plan) yaml file to the persistent file list
-
## 🔧 Fixes and Improvements
- Duplicate aider executeable path in settings fix
- Customize used docker image and tag in settings
- Removed Api Key setup for vertex ai provider

# Coding-Aider Plugin Release Notes - v1.2.5

## 🚀 New Features

- Vertex AI Provider Support (experimental) as configurable custom provider
- Structured Mode improvements:
    - Plan Continuation mode (enabled by default) will automatically continue working on plans as long as they are not finished
    - Button and View for managing plan context files
    - Added support for relative and absolute paths in context YAML file parsing

## 🔧 Fixes and Refinements

### Execution and Configuration
- Fixed issues with LLM provider selection that caused additional aider arguments to be ignored
- Fixed docker image tag field enabling
- Enhanced path normalization to prevent duplicate file entries
- Added file size and image filtering for token counting
