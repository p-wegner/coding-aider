# [Coding Aider Plan] Markdown Viewer Scrolling and Collapsible Panel Fixes

## Overview
The Markdown Viewer component in CodingAider currently has two significant usability issues that need to be addressed:
1. The auto-scrolling feature is overriding manual scrolling, causing the user's scroll position to be lost when content updates
2. The collapsible panels cannot be collapsed as intended

This plan outlines the necessary changes to fix these issues while maintaining the overall functionality of the Markdown Viewer system.

## Problem Description
### Auto-scrolling Issue
Currently, when content is updated in the Markdown Viewer, the system attempts to auto-scroll to the bottom regardless of whether the user has manually scrolled to a specific position. This creates a frustrating experience where users cannot maintain their reading position when new content is added.

The root cause appears to be in the JcefMarkdownRenderer class where the scrolling logic doesn't properly respect the user's manual scroll position. The current implementation forces scrolling to the bottom on content updates without properly checking if the user has intentionally scrolled to a different position.

### Collapsible Panels Issue
The collapsible panels feature, which should allow users to expand and collapse sections of content, is not functioning correctly. Users are unable to collapse panels, limiting the usefulness of this feature for managing large documents.

The issue likely stems from either:
1. Missing or incorrect event handlers for panel interaction
2. CSS/JavaScript implementation issues in the HTML template
3. Improper state management for panel expansion/collapse

## Goals
1. Fix the auto-scrolling behavior to respect user's manual scroll position
   - Only auto-scroll to bottom when appropriate (e.g., user was already at bottom)
   - Maintain scroll position when user has manually scrolled elsewhere
   - Implement proper detection of user-initiated scrolling vs. programmatic scrolling

2. Fix the collapsible panels functionality
   - Ensure panels can be both expanded and collapsed
   - Maintain panel state during content updates
   - Provide visual feedback (arrow indicators) for panel state

3. Ensure changes maintain compatibility with existing features
   - Theme switching
   - Content updates
   - DevTools integration
   - Performance considerations

## Additional Notes and Constraints
- Changes should be minimal and focused on fixing the specific issues
- Maintain the existing architecture and component structure
- Ensure backward compatibility with existing usage patterns
- Consider performance implications, especially for large documents
- Preserve existing theme support and visual styling

## References
- JcefMarkdownRenderer.kt - Core rendering component
- MarkdownDialog.kt - Dialog hosting the markdown viewer
- MarkdownThemeManager.kt - Theme management for the viewer
- MarkdownViewerPRD.md - Product requirements document
