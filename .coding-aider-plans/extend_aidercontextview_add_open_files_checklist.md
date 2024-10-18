[Coding Aider Plan - Checklist]

# Checklist: Extend AiderContextView to Add Currently Open Files

This checklist is part of the plan described
in [extend_aidercontextview_add_open_files.md](./extend_aidercontextview_add_open_files.md).

## Implementation Steps

1. [x] Add a new method in AiderContextView to retrieve currently open files from the IDE
    - [x] Use FileEditorManager to get open files
    - [x] Convert open files to FileData objects

2. [ ] Update the file selection dialog
    - [ ] Extend the existing add files option to open a popup with entries "From Project" (previous button action
      listener) and "Add Open Files" to the dialog
    - [ ] Implement action listener for the new option

3. [ ] Implement logic to add open files to AiderContextView
    - [ ] Filter out duplicate files
    - [ ] Add new files to allFiles list

4. [ ] Update UI components
    - [ ] Modify the "Add Files" action in AiderInputDialog
    - [ ] Update the file action group to include the new option

5. [ ] Update the tree view
    - [ ] Modify updateTree() method to handle new files
    - [ ] Ensure proper sorting and categorization of new files

6. [ ] Implement error handling
    - [ ] Handle cases where open files cannot be accessed
    - [ ] Provide user feedback for any issues

7. [ ] Add unit tests
    - [ ] Test new method for retrieving open files
    - [ ] Test updated file selection logic

8. [ ] Add integration tests
    - [ ] Test the entire flow of adding open files to AiderContextView

9. [ ] Update documentation
    - [ ] Add comments to new code
    - [ ] Update any existing documentation or README files

10. [ ] Perform manual testing
    - [ ] Test the new functionality in various scenarios within the IDE

11. [ ] Code cleanup and optimization
    - [ ] Refactor any duplicate code
    - [ ] Optimize for performance, especially with many open files

12. [ ] Final review and submission
    - [ ] Conduct a final code review
    - [ ] Prepare for pull request or commit
