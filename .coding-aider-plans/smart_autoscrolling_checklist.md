[Coding Aider Plan - Checklist]

- [x] Update the JavaScript in `markdown-viewer.js` to robustly track user scroll state and implement smart autoscrolling logic.
- [x] Ensure that the scroll state and panel expansion state are preserved across content updates.
- [x] Expose a JavaScript function to programmatically scroll to the bottom (`scrollToBottom()`).
- [x] Update the Kotlin JCEF renderer (`JcefMarkdownRenderer.kt`) to call the JavaScript scroll function after content updates, only if appropriate.
- [x] Update the `MarkdownViewer.kt` and `MarkdownDialog.kt` to allow triggering smart autoscroll from Kotlin.
- [ ] Test the feature with long outputs, rapid updates, and user interaction to ensure smooth and intuitive behavior.
- [ ] Update documentation and references as needed.
