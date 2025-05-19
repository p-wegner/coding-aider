# [Coding Aider Plan - Checklist] Markdown Renderer Fixes

## Lifecycle & Resource Management
- [ ] Remove unused JBCefJSQuery instance or implement proper usage
- [x] Fix theme-change listener leak in dispose() method
- [x] Convert non-daemon Timer instances to daemon timers or use AppExecutorUtil
- [x] Ensure dispose() runs on EDT with invokeAndWait
- [x] Fix DevTools handling to properly track and close all instances
- [x] Fix race condition between loadCompleted and pendingContent

## Threading / EDT Violations
- [x] Move contentProcessor.processMarkdown() to background thread
- [x] Ensure all Swing/CEF operations run on EDT with proper invokeLater
- [x] Fix nested invokeLater calls in executeJavaScript()

## JavaScript / HTML Template
- [ ] Fix originalUpdateContent undefined issue in template
- [ ] Improve panel-ID algorithm to maintain state across content updates
- [x] Enhance escapeJsString() to handle all special characters
- [ ] Add required CSS for collapsible panels and other UI elements
- [ ] Fix isScrolledToBottom() to work with updateContent() correctly
- [ ] Replace Base64 data URL with proper resource loading

## Performance Optimizations
- [ ] Implement incremental DOM updates instead of full innerHTML replacement
- [x] Optimize scrollToBottom() to avoid excessive Timer creation
- [x] Improve theme change handling to avoid full markdown re-parsing
- [ ] Optimize memory usage for large HTML content

## UX Enhancements
- [ ] Add CSS transitions for smooth collapsible panel animations
- [ ] Fix bottom-following logic to work correctly during rapid updates
- [ ] Implement proper fallback rendering when JCEF is unavailable
- [ ] Add keyboard navigation for collapsible headers
- [ ] Implement HTML sanitization for untrusted content

## MarkdownDialog Improvements
- [x] Replace Timer usage with daemon timers or AppExecutorUtil
- [ ] Fix EDT violations in dialog code
- [ ] Improve resource cleanup in window listeners
