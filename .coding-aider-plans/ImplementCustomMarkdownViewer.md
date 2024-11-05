[Coding Aider Plan]

# Implement Custom Markdown Viewer for MarkdownDialog

## Overview

Currently, the `MarkdownDialog` in our application relies on the JetBrains Markdown plugin (`org.intellij.plugins.markdown`) to render markdown content. However, this plugin is not well-maintained, which may lead to future issues and limitations. To ensure better control over markdown rendering and to have a more maintainable solution, we plan to implement our own markdown viewer within the `MarkdownDialog`.

## Goals

- **Replace Dependency**: Eliminate the reliance on the JetBrains Markdown plugin.
- **Custom Viewer**: Develop a custom markdown viewer that can render markdown content within the `MarkdownDialog`.
- **Proper Integration**: Ensure the new viewer integrates seamlessly with the existing codebase.
- **Maintain Functionality**: Preserve or enhance all current features of the `MarkdownDialog`, including real-time updates and user interactions.
- **Improve Maintainability**: Use well-maintained libraries and write clear, maintainable code.

## Implementation Details

1. **Select a Markdown Parsing Library**

   - Research and choose a well-maintained markdown parsing library suitable for Kotlin/JVM.
   - **Candidates**:
     - [Flexmark](https://github.com/vsch/flexmark-java): A comprehensive markdown parser that supports CommonMark and other extensions.
   - The library should be capable of converting markdown text to HTML.

2. **Create a Custom Markdown Viewer Component**

   - Implement a Swing component that can display HTML content.
   - Use `JEditorPane` or `JavaFX WebView` within a `JFXPanel` to render HTML content.
   - Ensure the viewer supports features like scrolling, resizing, and hyperlinks.

3. **Update `MarkdownDialog.kt`**

   - Replace the existing `MarkdownPreviewFileEditor` with the new custom markdown viewer component.
   - Modify methods related to content updates to use the new component.
   - Ensure that content updates are reflected in real-time.

4. **Remove `MarkdownPreviewFileEditorUtil.kt`**

   - Since this utility class is specific to the JetBrains Markdown plugin, remove it from the codebase.
   - Clean up any references to this utility class.

5. **Update Project Dependencies**

   - **Remove**: Delete the dependency on `org.intellij.plugins.markdown` from `build.gradle.kts`.
   - **Add**: Include the selected markdown parsing library (e.g., Flexmark) in the project dependencies.

6. **Testing and Validation**

   - Test the new markdown viewer with various markdown content to ensure correct rendering.
   - Validate that all features of the `MarkdownDialog` work as expected.
   - Check for any performance issues or memory leaks.

7. **Update Documentation**

   - Update `README.md` and any other relevant documentation to reflect the changes made.
   - Document the new components and any changes in usage or features.

8. **Code Review and Refactoring**

   - Perform a code review to ensure code quality and adherence to coding standards.
   - Refactor code where necessary for better readability and maintainability.

## References

- [Checklist for Implementing the Custom Markdown Viewer](ImplementCustomMarkdownViewer_Checklist.md)