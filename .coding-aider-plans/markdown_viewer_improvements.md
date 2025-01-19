[Coding Aider Plan]

# Markdown Viewer Improvements

## Overview
The current markdown viewer implementation has several usability issues that need to be addressed to improve the user experience:
1. Window size and content mismatch requiring unnecessary scrolling
2. Loss of scroll position during content streaming
3. Improper highlighting and line break handling in summary/intention blocks

## Problem Description
The current implementation suffers from three main issues:

1. Window Size Mismatch:
   - Content doesn't fit properly within the visible area
   - Users need to manually scroll to see all content
   - Window dimensions don't adapt well to content

2. Scroll Position Issues:
   - Scroll position resets during content updates
   - Makes it difficult to follow streaming content
   - Poor user experience during long-running processes

3. Formatting Problems:
   - Summary and intention blocks lack proper highlighting
   - Line breaks aren't handled correctly
   - Visual hierarchy needs improvement

## Goals
1. Improve window sizing and content display:
   - Implement proper initial window sizing
   - Add dynamic content area adjustment
   - Optimize scroll bar behavior

2. Fix scroll position management:
   - Maintain scroll position during content updates
   - Implement smart auto-scrolling
   - Add user scroll position memory

3. Enhance formatting:
   - Improve summary/intention block styling
   - Fix line break handling
   - Enhance visual hierarchy

## Additional Notes and Constraints
- Must maintain compatibility with existing markdown processing
- Should preserve current theming capabilities
- Need to handle both light and dark themes
- Performance considerations for streaming updates

## References
- Current implementation in MarkdownDialog.kt
- JCEF Browser documentation
- Flexmark HTML rendering documentation
