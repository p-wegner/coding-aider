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

### 4. Remove `CustomMarkdownViewer.kt`

- [x] Remove `CustomMarkdownViewer.kt` from the project.
- [x] Search for any references to this utility and remove them.
- [x] Test to ensure removal does not affect other parts of the application.

### 5. Update Project Dependencies

- [x] In `build.gradle.kts`:
    - [x] Remove the `org.intellij.plugins.markdown` plugin dependency.
    - [x] Add the dependency for the selected markdown parsing library.

### 5. Improve UX

- [x] Implement autoscrolling behaviour like in shell applications.
  If the user has scrolled, autoscrolling is off. If view is at the bottom of the content, autoscrolling is turned on.

### 7. Update Documentation

- [ ] Update `README.md` to remove references to the JetBrains Markdown plugin.
- [ ] Document the new markdown viewer component:
    - [ ] Usage and features.
    - [ ] Any limitations or caveats.
- [ ] Update any other relevant documentation or comments in the code.
