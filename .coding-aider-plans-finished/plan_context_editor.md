[Coding Aider Plan]

# Plan Context Editor Feature

## Overview
Add a button to the PlanViewer that allows users to edit the context files of a plan through a dialog similar to the PersistentFilesPanel. This will make it easier to manage which files are included in the context of a plan.

## Problem Description
Currently, the context.yaml files for plans can only be edited manually. This is error-prone and not user-friendly. Users need an easy way to add or remove files from a plan's context directly from the UI.

## Goals
1. Add an "Edit Context" button to the PlanViewer toolbar
2. Create a dialog with file management UI similar to PersistentFilesPanel
3. Allow adding/removing files from plan context
4. Maintain proper YAML file format for context files
5. Provide immediate feedback on changes
6. Support both single and multiple file selection

## Additional Notes and Constraints
- The UI should match existing IntelliJ design patterns
- Changes should be saved immediately
- The dialog should show the full file paths
- Support for read-only flag on files
- Must handle file existence validation
- Should refresh plan view after context changes

## References
[Checklist](plan_context_editor_checklist.md)

The implementation will reuse components from the PersistentFilesPanel to maintain consistency in the UI and reduce code duplication.
