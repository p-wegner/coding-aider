[Coding Aider Plan]

## Overview
Implement UI components to allow users to create plans from completed Aider actions.

## Problem Description
Users need clear, intuitive UI affordances to convert completed actions into structured plans. These UI elements must be accessible at the right time in the user workflow.

## Goals
1. Add a "Create Plan" button to the output dialog
2. Add a "Create Plan from Last Command" button to the running commands toolwindow
3. Ensure UI elements are only enabled when appropriate (after command completion)
4. Provide visual feedback during plan creation process

## Implementation Details
### Output Dialog Button
- Add a button to the MarkdownDialog class
- Position it alongside existing action buttons
- Only enable after command has completed
- Trigger plan creation process when clicked

### Running Commands Toolwindow Button
- Add a button to the running commands toolwindow
- Enable only when there's a completed command available
- Provide tooltip explaining the functionality
- Trigger the same plan creation process as the dialog button

## Additional Notes and Constraints
- UI elements should follow IntelliJ design guidelines
- Consider accessibility requirements
- Provide appropriate error handling and user feedback
- Ensure UI state is properly updated after plan creation

## References
- MarkdownDialog.kt for dialog UI modifications
- RunningCommandService.kt for toolwindow integration
