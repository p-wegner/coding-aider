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

## Previous Versions

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
