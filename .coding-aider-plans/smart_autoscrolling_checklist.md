[Coding Aider Plan - Checklist]

- [ ] Update the JavaScript in `markdown-viewer.js` to robustly track user scroll state and implement smart autoscrolling logic.
- [ ] Ensure that the scroll state and panel expansion state are preserved across content updates.
- [ ] Expose a JavaScript function to programmatically scroll to the bottom (`scrollToBottom()`).
- [ ] Update the Kotlin JCEF renderer (`JcefMarkdownRenderer.kt`) to call the JavaScript scroll function after content updates, only if appropriate.
- [ ] Update the `MarkdownViewer.kt` and `MarkdownDialog.kt` to allow triggering smart autoscroll from Kotlin.
- [ ] Test the feature with long outputs, rapid updates, and user interaction to ensure smooth and intuitive behavior.
- [ ] Update documentation and references as needed.
