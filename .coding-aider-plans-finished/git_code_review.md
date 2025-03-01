[Coding Aider Plan]
  
## Overview
This plan introduces a new action that leverages Git features to enable code reviews between branches or commits using Aider. The new action will offer a dialog where users select two branches or tags and enter a custom prompt. The action will then determine all differing files using Git (or Git4Idea integrations) and package the git diff together with the user's prompt into CommandData for further processing by Aider.

## Problem Description
Developers need to efficiently review code changes between branches or commits. Manually gathering the differences and synthesizing feedback is error-prone and time-consuming. There is a need for a seamless integration within the IDE that directly leverages Git information and passes it to Aider for automated code review.

## Goals
- Create a new action accessible from the IDE.
- Open a dialog for branch or tag selection and text area entry for a prompt.
- Use Git (or Git4Idea API features) to identify all differing files between the selected points.
- Bundle the Git diff along with the user's prompt into a single CommandData message.
- Ensure the new action integrates with the existing Aider workflow seamlessly.

## Additional Notes and Constraints
- The UI should be intuitive and consistent with existing dialogs.
- The implementation should leverage existing Git utilities (or Git4Idea services).
- The action must handle error cases (e.g. invalid branch names, no diff found).
- Testing should verify that the generated CommandData has both the Git diff and the user prompt.

## References
- [Aider CommandData Specification](../src/main/kotlin/de/andrena/codingaider/command/CommandData.kt)

