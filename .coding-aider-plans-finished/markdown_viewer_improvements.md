[Coding Aider Plan] Enhanced Code Block Support for CustomMarkdownViewer

## Overview
Improve the CustomMarkdownViewer to provide advanced rendering and interaction capabilities for code blocks in markdown files, focusing on syntax highlighting, line numbering, and code-related features.

## Problem Description
The current CustomMarkdownViewer lacks sophisticated code block rendering and interaction features. Developers need:
- Syntax highlighting for multiple programming languages
- Line numbering
- Code block copy functionality
- Responsive code block display
- Support for different code block styles and themes

## Goals
1. Implement multi-language syntax highlighting
2. Add line numbering to code blocks
3. Create a copy-to-clipboard button for code blocks
4. Improve code block readability and styling
5. Support responsive design for code blocks

## Additional Notes and Constraints
- Use existing libraries like Prism.js or highlight.js for syntax highlighting
- Maintain compatibility with current markdown rendering
- Ensure performance and minimal overhead
- Support dark and light theme variations for code blocks

## References
- Flexmark Markdown Parser Documentation
- IntelliJ Platform Plugin SDK
- Syntax Highlighting Libraries Comparison

## Proposed Technical Approach
1. Extend FlexMark extensions to capture code block metadata
2. Integrate syntax highlighting library
3. Create custom HTML/CSS for enhanced code block rendering
4. Implement JavaScript for interactive features
5. Add configuration options for code block appearance
