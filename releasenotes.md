# Coding-Aider Plugin Release Notes - v1.2.7

## 🚀 New Features

- Working Directory Support:
  - Configure specific working directory for Aider operations
  - Files outside working directory are excluded from context
  - Path normalization and validation for working directory paths
  - Working directory panel in tool window
- Improved Markdown Viewer:
  - Enhanced JCEF browser integration with theme support
  - Improved scrolling behavior and layout responsiveness
  - Better styling for code blocks and search/replace sections
  - Smooth scrolling animation with error handling
- Enhanced Plan Mode:
  - Support for hierarchical plans with parent-child relationships
  - Tree visualization for plan hierarchy
  - Plan refinement capabilities
  - Improved plan continuation logic
  - Archive functionality for completed plans
- Image Handling:
  - Improved clipboard image support in input dialog
  - Automatic image saving and persistent file management
- Output Formatting:
  - Added HTML escaping for search/replace blocks
  - Enhanced styling for summary blocks
  - Improved history parsing and display

## 🔧 Fixes and Improvements

- Fixed path normalization for working directory validation
- Resolved XML block encoding issues in markdown viewer
- Improved scroll position handling during content updates
- Enhanced theme compatibility and styling
- Fixed initialization issues with JCEF browser
- Improved error handling and null safety
- Updated documentation for new features
- Fixed tests and updated Aider version requirements

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
