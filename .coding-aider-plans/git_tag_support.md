[Coding Aider Plan]

# Git Tag Support in Code Review Dialog

## Overview
This plan implements support for selecting git tags in the GitCodeReviewDialog, allowing users to compare code between tags, branches, or any git refs.

## Problem Description
Currently, the GitCodeReviewDialog only supports selecting branches when comparing code changes. Users need the ability to:
1. Select tags as base/target references
2. See both branches and tags in the completion dropdown
3. Clearly understand what type of reference they're selecting

## Goals
1. Modify GitRefComboBox to always show both branches and tags in completion
2. Update the UI to clearly indicate tag vs branch selections
3. Remove the ref type dropdown to simplify the interface
4. Maintain all existing functionality
5. Ensure consistent behavior with the GitLogCodeReviewAction

## Additional Notes and Constraints
- Must maintain backward compatibility
- Should match existing UI styling
- Need to handle cases where repository has no tags
- Should work with all git operations (diff, comparison, etc.)

## References
- [GitRefComboBox.kt](src/main/kotlin/de/andrena/codingaider/actions/git/GitRefComboBox.kt)
- [GitCodeReviewDialog.kt](src/main/kotlin/de/andrena/codingaider/actions/git/GitCodeReviewDialog.kt)
- [GitDiffUtils.kt](src/main/kotlin/de/andrena/codingaider/utils/GitDiffUtils.kt)
