[Coding Aider Plan]

# Smart Autoscrolling for Markdown Viewer

## Overview

This plan introduces "smart autoscrolling" to the Markdown Viewer system in the CodingAider IDE plugin. The goal is to provide an intelligent scrolling experience that automatically scrolls to the bottom for new content, but preserves the user's manual scroll position and panel expansion state when appropriate. This feature is especially useful for long-running command outputs, collaborative editing, and interactive markdown content.

## Problem Description

Currently, the Markdown Viewer either always scrolls to the bottom on content updates or does not scroll at all, which can be disruptive to users who are reading or interacting with earlier content. Users may lose their place if the view jumps unexpectedly, or may miss new output if the view does not follow the latest content. There is also a need to preserve the expansion state of collapsible panels and the user's scroll position during content updates.

## Goals

- Detect whether the user has manually scrolled away from the bottom of the content.
- Automatically scroll to the bottom only if the user was already at the bottom before the update, or if the update is triggered programmatically.
- Preserve the user's manual scroll position and panel expansion state during content updates.
- Provide a smooth and intuitive scrolling experience, especially for long outputs and interactive content.
- Integrate seamlessly with the existing Markdown Viewer JavaScript and Kotlin code.

## Additional Notes and Constraints

- The implementation should work for both JCEF-based and fallback HTML renderers.
- The JavaScript logic should be robust against race conditions and DOM update timing issues.
- The Kotlin-side should expose a method to programmatically scroll to the bottom when needed.
- The feature should not interfere with keyboard navigation or accessibility.
- The solution must be cross-platform and not rely on browser-specific APIs.