[Coding Aider Plan]

# Extend AiderContextView to Add Currently Open Files

## Feature Description

Extend the AiderContextView.kt file to provide an option to add all currently open files from the IDE to the
AiderContextView files. The current "Add Files" functionality should be extended to behave similarly to the feature
shown in the provided image, with an entry to add currently open files.

## Implementation Details

1. Modify the AiderContextView class to include a new method for retrieving currently open files from the IDE.
2. Update the existing file selection dialog to include an option for adding currently open files.
3. Implement the logic to add the open files to the AiderContextView's file list.
4. Update the UI to reflect the new option in the file selection dialog.
   The new feature should be combined with the existing add files button. There should be one menu button that opens a
   popup with the two options: Add Files and Add Open Files.
5. Ensure proper handling of duplicate files and updating the tree view.

## Dependencies

- IntelliJ Platform SDK for accessing currently open files in the IDE.
- Existing AiderContextView implementation.

## Potential Challenges

- Handling of unsaved changes in open files.
- Ensuring performance with a large number of open files.

For the detailed implementation steps, refer to the [checklist](./extend_aidercontextview_add_open_files_checklist.md).
