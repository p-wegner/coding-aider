# [Coding Aider Plan - Checklist]

## Critical Severity Fixes
- [x] Consolidate window adapters in MarkdownDialog to prevent double event firing
- [x] Replace non-daemon Timer threads with Swing timers or properly managed daemon threads
- [x] Ensure all UI updates are properly wrapped in invokeLater
- [x] Add null/parent check before removing browser component in switchToFallbackEditor
- [x] Prevent multiple abortCommand invocations with proper guards

## High Severity Fixes
- [x] Fix duplicate keepOpenButton allocation
- [x] Remove or properly implement refreshTimer (currently dead code)
- [x] Add isDisplayable check in updateProgress to prevent updates to disposed dialog
- [x] Fix exponential back-off integer overflow in MarkdownJcefViewer
- [ ] Fix load handler registration with correct browser instance

## Medium Severity Fixes
- [x] Fix timer display showing "0 seconds" for a whole second
- [x] Optimize HTML reloading to prevent flicker when theme changes
- [x] Add proper null checks for ProjectManager.getInstance().openProjects
- [x] Improve regex performance in processSearchReplaceBlocks

## Low Severity Improvements
- [x] Remove duplicate mnemonic assignments for closeButton
- [x] Consider using StringEscapeUtils for HTML escaping
- [x] Add locale parameter to ResourceBundle.getBundle
- [x] Optimize redundant string conversions
