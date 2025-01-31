[Coding Aider Plan]

# Collapsible Initial Command Panel Implementation

## Overview
Add support for displaying the initial aider command in a collapsible panel within the markdown output view to improve UI cleanliness and user experience.

## Problem Description
The current implementation shows the initial command text directly in the output panel, which:
- Takes up significant vertical space
- Distracts from the main output content
- Provides poor visual hierarchy for command sequence

## Goals
1. Implement collapsible panel component for initial commands
2. Maintain backwards compatibility with existing output formatting
3. Ensure smooth animation transitions
4. Preserve functionality of command text selection/copying

## Additional Notes and Constraints
- Must work with both JCEF and fallback JEditorPane renderers
- Should follow IntelliJ platform UI guidelines
- Needs to handle dark/light theme variations
- Must maintain current scroll behavior logic

## References
- `MarkdownJcefViewer.kt` - Main markdown rendering component
- `MarkdownDialog.kt` - Output view container
- `IDEBasedExecutor.kt` - Command execution entry point

<!-- SUBPLAN:collapsible_command_panel_styling -->
[Subplan: Styling Implementation](collapsible_command_panel_styling.md)
<!-- END_SUBPLAN -->

<!-- SUBPLAN:collapsible_command_panel_logic -->
[Subplan: Interaction Logic](collapsible_command_panel_logic.md)
<!-- END_SUBPLAN -->
