[Coding Aider Plan]

# Implement Markdown Viewer Audit Fixes

## Overview

This plan outlines the steps required to address the issues identified in the recent audit of the Markdown Viewer system components (`MarkdownDialog.kt` and `MarkdownJcefViewer.kt`), as detailed in the `prompt.txt` audit report. The goal is to fix critical bugs, high-priority issues, and address medium and low-priority findings to improve the stability, correctness, and maintainability of the viewer.

## Problem Description

The audit report (`prompt.txt`) highlighted several issues ranging from critical bugs causing rendering failures and incorrect auto-close timing to high-priority problems like dead code and incorrect JCEF handler usage. Medium and low-priority issues related to code style, error handling, and minor logic flaws were also noted. These issues impact the user experience, reliability, and code quality of the Markdown Viewer.

## Goals

The primary goals of this plan are to:
- Resolve the critical rendering bug in `MarkdownJcefViewer.setMarkdown`.
- Correct the timing and countdown display for the auto-close timer in `MarkdownDialog.startAutoCloseTimer`.
- Remove unused imports and dead code from `MarkdownDialog`.
- Fix the content update logic in `MarkdownJcefViewer.updateContent` to handle identical consecutive updates correctly.
- Correct the JCEF load handler registration in `MarkdownJcefViewer.initJcefBrowser`.
- Address the medium and low-priority issues related to exponential backoff comments, integer truncation, regex backtracking risk, HTML escaping robustness, and code style consistency.
- Improve the overall robustness and maintainability of the Markdown Viewer code.

## Additional Notes and Constraints

- All changes must adhere to the existing code style and project conventions.
- The implementation should be limited to addressing the specific issues listed in the `prompt.txt` audit report.
- Plan files must be created and committed before starting any code modifications.
- The defined editing format (filepath above search blocks) must be used for code changes.

## References

- Audit Report: `prompt.txt`
- Checklist: [Markdown Viewer Audit Fixes Checklist](markdown_viewer_audit_fixes_checklist.md)
- Context Files: [Markdown Viewer Audit Fixes Context](markdown_viewer_audit_fixes_context.yaml)

The following files are primarily affected by this plan:
- `src/main/kotlin/de/andrena/codingaider/outputview/MarkdownDialog.kt`
- `src/main/kotlin/de/andrena/codingaider/outputview/MarkdownJcefViewer.kt`
- `build.gradle.kts` (mentioned in audit, though related fix is in `MarkdownDialog.kt`)
