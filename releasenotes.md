# Coding-Aider Plugin Release Notes - v1.2.7



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
