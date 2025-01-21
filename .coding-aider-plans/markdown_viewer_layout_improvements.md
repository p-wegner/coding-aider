[Coding Aider Plan]
# Markdown Viewer Layout Improvements

## Overview
Improve the layout and scrolling behavior of the MarkdownJcefViewer component to create a better user experience. The changes should make the content properly responsive, eliminate nested scrollbars, and allow proper window resizing.

## Problem Description
Current issues:
1. Content doesn't resize properly with window
2. Unwanted horizontal scrollbars appear
3. Nested scrollbars between IDE and component
4. Limited horizontal resizing capability

## Goals
1. Make content fully responsive to window size
2. Allow both horizontal and vertical window resizing
3. Eliminate nested scrollbars
4. Ensure proper text wrapping in code blocks
5. Maintain readability across window sizes

## Additional Notes and Constraints
- Must maintain dark/light theme compatibility
- Should preserve existing styling improvements
- Need to handle both JCEF and fallback modes
- Ensure smooth transition during resizing

## References
- Existing layout code in MarkdownJcefViewer.kt
- Current CSS styles in markdown conversion
- Swing layout manager documentation

