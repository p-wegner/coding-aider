# [Coding Aider Plan]

## Overview
This plan addresses critical bugs and issues in the Markdown Viewer system, specifically in the `MarkdownDialog` and `MarkdownJcefViewer` components. The bugs range from critical severity (affecting stability, resources, or user data) to low severity (clean-up and polish). The fixes will improve stability, prevent memory leaks, fix UI glitches, and enhance overall performance.

## Problem Description
The bug audit identified several issues:

1. **Critical Severity**:
   - Duplicate window event handlers causing double execution of abort commands
   - Non-daemon Timer threads never stopped, causing memory and thread leaks
   - UI updates from non-EDT threads causing random exceptions
   - Fallback switch may run while browser already disposed
   - Multiple abortCommand invocations possible

2. **High Severity**:
   - keepOpenButton allocated twice
   - refreshTimer cancelled but never started (dead code)
   - updateProgress() can post to already disposed dialog
   - Exponential back-off integer overflow
   - Load handler registered with wrong browser instance

3. **Medium and Low Severity**:
   - Timer display showing "0 seconds" for a whole second
   - Unnecessary HTML reloads causing flicker
   - Missing null checks in headless unit tests
   - Greedy regexes potentially causing performance issues
   - Various code smells and redundancies

## Goals
1. Fix all critical severity bugs to prevent crashes, memory leaks, and thread leaks
2. Address high severity bugs to improve stability and user experience
3. Implement medium severity fixes to enhance edge case handling
4. Apply low severity improvements for code cleanliness and maintainability
5. Ensure all fixes follow best practices for Swing/EDT threading
6. Maintain backward compatibility with existing functionality

## Additional Notes and Constraints
- All UI updates must be properly wrapped in `invokeLater` to ensure EDT compliance
- Timer implementations should be replaced with Swing timers or properly managed daemon threads
- Browser component disposal must be handled carefully to prevent exceptions
- Changes should maintain the existing functionality while improving stability
- The fixes should be implemented in a way that doesn't break existing features

## References
- [IntelliJ Platform UI Guidelines](https://jetbrains.github.io/ui/principles/platform_theme_colors/)
- [Swing Threading Rules](https://docs.oracle.com/javase/tutorial/uiswing/concurrency/index.html)
- [JCEF Documentation](https://plugins.jetbrains.com/docs/intellij/jcef.html)
