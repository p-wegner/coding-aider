# [Coding Aider Plan - Checklist] Markdown Viewer Scrolling and Collapsible Panel Fixes

## Auto-scrolling Fixes
- [ ] Modify JcefMarkdownRenderer to properly track user vs. programmatic scrolling
- [ ] Update scroll position preservation logic in content updates
- [ ] Implement proper detection of when user is at bottom of content
- [ ] Fix shouldAutoScroll flag handling to respect user's scroll position

## Collapsible Panels Fixes
- [ ] Debug and fix event handlers for panel expansion/collapse
- [ ] Ensure proper CSS/styling for collapsed/expanded states
- [ ] Fix arrow indicator updates for panel state
- [ ] Implement proper state preservation during content updates