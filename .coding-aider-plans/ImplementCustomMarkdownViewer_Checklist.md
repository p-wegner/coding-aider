[Coding Aider Plan - Checklist]

# Checklist for Implementing the Custom Markdown Viewer

Refer to the [Implement Custom Markdown Viewer for MarkdownDialog](ImplementCustomMarkdownViewer.md) plan.

## Tasks

### 1. Select a Markdown Parsing Library

- [x] Research available markdown parsing libraries for Kotlin/JVM.
- [x] Evaluate libraries such as:
    - [x] **Flexmark**: Supports CommonMark and many extensions.
- [x] Choose the most suitable library.
- [x] Add the selected library to the project dependencies in `build.gradle.kts`.

### 2. Create a Custom Markdown Viewer Component

- [x] Implement a Swing component to render HTML content.
    - [x] Use `JEditorPane` or `JFXPanel` with `WebView`.
- [x] Ensure the component can:
    - [x] Display HTML generated from markdown.
    - [x] Handle scrolling and resizing.
    - [x] Support hyperlinks and other interactive elements.
- [x] Test the component independently with sample HTML content.

### 3. Update `MarkdownDialog.kt`

- [x] Replace `MarkdownPreviewFileEditor` with the custom markdown viewer component.
- [x] Update methods to load and display markdown content:
    - [x] Convert markdown text to HTML using the selected library.
    - [x] Render the HTML content in the custom viewer.
- [x] Ensure real-time updates work correctly when content changes.
- [x] Handle any events or interactions previously managed by `MarkdownPreviewFileEditor`.

### 4. Remove `MarkdownPreviewFileEditorUtil.kt`

- [x] Remove `MarkdownPreviewFileEditorUtil.kt` from the project.
- [x] Search for any references to this utility and remove them.
- [x] Test to ensure removal does not affect other parts of the application.

### 5. Update Project Dependencies

- [ ] In `build.gradle.kts`:
    - [ ] Remove the `org.intellij.plugins.markdown` plugin dependency.
    - [ ] Add the dependency for the selected markdown parsing library.

### 6. Testing and Validation

- [ ] Test the updated `MarkdownDialog` with various markdown inputs, including:
    - [ ] Basic markdown syntax (headings, lists, code blocks).
    - [ ] Advanced features (tables, images, links).
- [ ] Verify real-time updates:
    - [ ] Content changes reflect immediately.
- [ ] Check for performance issues:
    - [ ] No significant lag or memory usage spikes.
- [ ] Ensure user interactions work as expected:
    - [ ] Scrolling, resizing, and focus behavior.

### 7. Update Documentation

- [ ] Update `README.md` to remove references to the JetBrains Markdown plugin.
- [ ] Document the new markdown viewer component:
    - [ ] Usage and features.
    - [ ] Any limitations or caveats.
- [ ] Update any other relevant documentation or comments in the code.

### 8. Code Review and Merge

- [ ] Perform a thorough code review.
    - [ ] Check for code quality, adherence to standards, and potential issues.
- [ ] Make any necessary refactoring or fixes based on the review.
- [ ] Commit all changes with clear commit messages.
- [ ] Merge the changes into the main branch after approval.

### 9. Post-Implementation

- [ ] Monitor for any issues or bugs reported after deployment.
- [ ] Be prepared to make quick fixes if necessary.
- [ ] Consider writing unit tests for the new components if applicable.
