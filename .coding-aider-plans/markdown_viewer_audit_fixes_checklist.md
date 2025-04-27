[Coding Aider Plan - Checklist]

# Markdown Viewer Audit Fixes Checklist

This checklist details the atomic tasks required to implement the fixes outlined in the main plan, based on the findings in the `prompt.txt` audit report.

## Critical Issues

- [ ] **#1:** Modify `MarkdownJcefViewer.setMarkdown` and `updateContent` to correctly handle non-blank input and consecutive identical updates, ensuring content is always rendered when needed.
- [ ] **#2:** Adjust `MarkdownDialog.startAutoCloseTimer` to start the countdown display correctly (e.g., start task after 1 second or update UI before decrementing).

## High Priority Issues

- [ ] **#3:** Remove the unused `import com.sun.java.accessibility.util.AWTEventMonitor.addActionListener` from `MarkdownDialog.kt`.
- [ ] **#4:** Remove the unused `resizeTimer: Timer?` property and its cleanup from `MarkdownDialog.kt`.
- [ ] **#5:** Remove the unused `refreshTimer: Timer?` property and its cleanup from `MarkdownDialog.kt`.
- [ ] **#6:** Modify `MarkdownJcefViewer.updateContent` to compare against a `lastRenderedContent` or similar mechanism to allow re-pushing identical markdown when necessary (e.g., theme change).
- [ ] **#7:** Correct the second argument passed to `client.addLoadHandler` in `MarkdownJcefViewer.initJcefBrowser` to be `null` or `jbCefBrowser!!.cefBrowser`.

## Medium Priority Issues

- [ ] **#8:** Update the comment regarding exponential backoff in `MarkdownJcefViewer.initJcefBrowser` to accurately reflect the capped delay, or remove the cap.
- [ ] **#9:** Consider using `kotlin.math.min(delay, Int.MAX_VALUE.toLong()).toInt()` in `MarkdownDialog.focus()` to prevent potential `Int` truncation issues with very large delays.
- [ ] **#10:** Evaluate and potentially improve the regex patterns in `MarkdownJcefViewer.processSearchReplaceBlocks` to mitigate catastrophic backtracking risk, possibly using possessive quantifiers or alternative parsing.

## Low Priority Issues

- [ ] **#11:** (Duplicate of #3) Ensure the unused `ActionListener` import is removed from `MarkdownDialog.kt`.
- [ ] **#12:** Add defensive error handling (e.g., `try-catch NoClassDefFoundError`) around the use of `org.apache.commons.text.StringEscapeUtils` in `MarkdownJcefViewer.escapeHtml` for compatibility with 3rd-party IDEs.
- [ ] **#13:** Standardize the usage of `invokeLater` in `MarkdownDialog.kt` and `MarkdownJcefViewer.kt` to either use the static import or `SwingUtilities.invokeLater` consistently.
