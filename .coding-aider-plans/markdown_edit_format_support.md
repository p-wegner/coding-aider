[Coding Aider Plan]
# Improved Edit Format Support for Markdown Viewer

## Overview
The CustomMarkdownViewer needs to be enhanced to properly handle and display different edit formats (whole, diff, diff-fenced, udiff) as proper markdown, ensuring code blocks are correctly identified and formatted regardless of the input format.

## Problem Description
Currently, the CustomMarkdownViewer doesn't properly handle all edit formats consistently. Code blocks may not be correctly identified and formatted depending on the specific edit format used. This leads to inconsistent and potentially incorrect display of content.

The main issues are:
- Different edit formats (whole, diff, diff-fenced, udiff) use varying syntax
- Code blocks within edit formats may not be properly detected
- Search/replace markers may be displayed as plain text instead of being properly formatted
- File paths and edit format markers may interfere with markdown parsing

## Goals
1. Detect and parse different edit formats (whole, diff, diff-fenced, udiff)
2. Convert edit format content to proper markdown before rendering
3. Ensure code blocks are consistently formatted across all edit formats
4. Maintain proper syntax highlighting for code sections
5. Preserve file path information while keeping it visually distinct
6. Handle nested code blocks within edit format blocks

## Additional Notes and Constraints
- Must maintain compatibility with existing markdown features
- Should preserve dark/light theme support
- Need to handle multiple edit format blocks in a single document
- Should gracefully handle malformed edit format blocks
- Must maintain existing hyperlinking functionality

## References
- [Checklist](markdown_edit_format_support_checklist.md)
- Current edit formats:
  - Whole format
  - Diff format
  - Diff-fenced format
  - Udiff format
