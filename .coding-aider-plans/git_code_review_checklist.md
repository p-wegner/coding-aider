[Coding Aider Plan - Checklist]

# Implementation Checklist

- [x] Define the new action class for Git code review and place it in the actions package.
- [x] Create the dialog for branch/tag selection and prompt entry.
- [x] Integrate Git4Idea APIs to compute file differences between branches/tags.
- [x] Bundle the git diff output with the user prompt into a CommandData instance.
- [x] Implement error handling and edge case management:
  - [x] Add Git reference validation
  - [x] Improve error messages in dialog
  - [x] Add Git availability check
  - [x] Handle empty diff results
  - [x] Enhance review prompt structure
- [x] Write unit tests and UI tests to validate functionality:
  - [x] Test GitCodeReviewAction functionality
  - [x] Test GitCodeReviewDialog behavior
  - [x] Test GitDiffUtils error handling and file processing
