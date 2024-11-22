[Coding Aider Plan]

# Add Context Menu Entry for Failed Gradle Run Results

## Overview
Add functionality to fix Gradle build/run failures using Aider, similar to the existing compile error fixing feature. This will allow users to quickly address Gradle errors through the context menu in the run results window.

## Feature Details
- Add new actions for fixing Gradle errors (both direct and interactive modes)
- Integrate with IntelliJ's run tool window
- Reuse existing error fixing patterns from FixCompileErrorAction
- Support both quick fix and interactive modes like the compile error feature

## Technical Details
1. Create new action classes extending BaseFixCompileErrorAction
2. Add menu entries to the run tool window's context menu
3. Extract error messages from Gradle run results
4. Implement error fixing logic similar to compile error handling
5. Add intention actions for quick access

## Dependencies
- Existing FixCompileErrorAction implementation
- IntelliJ Platform SDK for run tool window integration
- Aider integration components

See detailed implementation steps in [fix_gradle_errors_checklist.md](fix_gradle_errors_checklist.md)
