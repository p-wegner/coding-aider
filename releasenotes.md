# Coding-Aider Plugin Release Notes - v1.2.8

## 🌟 Key Highlights

- **LM Studio Integration**: Full support for local LM Studio instances with OpenAI-compatible API endpoints
- **Git Code Review**: New feature for AI-powered code review between branches/commits
- **Enhanced TODO Management**: Quick and interactive TODO fix actions

## 🚀 New Features

- LM Studio Provider Support:
  - OpenAI-compatible API endpoints integration
  - Optional API key support for secured instances
  - Default local URL configuration (http://localhost:1234/v1)
  - Docker network host mode support

- Git Code Review:
  - Branch and commit comparison
  - Customizable review focus areas
  - Detailed diff analysis
  - Summarized feedback with actionable insights

- Working Directory Enhancements:
  - Improved directory validation
  - Visual feedback in tool window
  - Path normalization and canonical path handling
  - Proper error messages for invalid selections

- TODO Management:
  - Quick fix action for TODOs
  - Interactive mode with custom prompts
  - File context awareness
  - Integration with existing inspection system

## 💡 Improvements

- Image Handling:
  - Enhanced drag & drop support
  - Visual feedback during drag operations
  - Better file type detection
  - Improved error handling

- UI Enhancements:
  - Collapsible command panels in output
  - Better UTF-8 handling in markdown viewer
  - Dialog positioning on same screen as IDE
  - Improved scroll behavior

- Performance & Reliability:
  - Local model cost mapping option
  - Reasoning effort settings for specific models
  - Better error handling in Git operations
  - Improved file system operations

## 🔧 Bug Fixes
- Fixed encoding issues with UTF-8 in markdown viewer
- Fixed dialog positioning across multiple screens
- Improved file path handling in persistent files
- Enhanced error messages for Git operations

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
