[Coding Aider Plan - Checklist]
# Implementation Checklist for Edit Format Support

## Analysis & Setup
- [ ] Review current markdown parsing implementation
- [ ] Identify all edit format patterns to support
- [ ] Document required regex patterns for format detection

## Core Implementation
- [ ] Create EditFormatDetector class/utility
  - [ ] Implement detection for whole format
  - [ ] Implement detection for diff format
  - [ ] Implement detection for diff-fenced format
  - [ ] Implement detection for udiff format
- [ ] Create EditFormatConverter class/utility
  - [ ] Implement conversion for whole format
  - [ ] Implement conversion for diff format
  - [ ] Implement conversion for diff-fenced format
  - [ ] Implement conversion for udiff format
- [ ] Update CustomMarkdownViewer
  - [ ] Add preprocessing step for edit format detection
  - [ ] Integrate format conversion before markdown parsing
  - [ ] Enhance code block handling
  - [ ] Update CSS styles for edit format elements

## Testing
- [ ] Create test cases for each edit format
- [ ] Verify proper code block formatting
- [ ] Test dark/light theme compatibility
- [ ] Validate nested code block handling
- [ ] Check file path display formatting

## Documentation
- [ ] Update code documentation
- [ ] Add examples for each supported format
- [ ] Document any new configuration options

## Final Steps
- [ ] Review performance impact
- [ ] Clean up temporary code
- [ ] Update unit tests
