[Coding Aider Plan]

# Hierarchical Plans Support

## Overview

Add support for hierarchical plans in the Coding Aider plugin, allowing plans to reference and contain subplans. This
feature will enhance organization of complex tasks and provide better visualization and execution control of nested plan
structures.

## Problem Description

Currently, plans are flat structures without support for hierarchical relationships. This limits the ability to:

- Break down complex features into manageable subplans
- Visualize relationships between parent and child plans
- Execute plans in a hierarchical manner
- Track progress across plan hierarchies
- Create nested plans from a single prompt

## Goals

1. Implement data structures to support hierarchical plan relationships
2. Enhance plan visualization to show parent-child relationships
3. Modify plan execution to handle hierarchical structures
4. Support creation of nested plans from single prompts
5. Allow refinement of existing plans into subplans
6. Maintain proper state tracking across plan hierarchies

## Additional Notes and Constraints

- Plans can have arbitrary depth of nesting
- When a subplan completes, execution should continue with next sibling or parent plan
- UI should clearly show plan hierarchies
- Existing plan file structure (.md, _checklist.md, _context.yaml) should be maintained
- Plan references should use relative paths within the .coding-aider-plans directory
- Subplans should use _subplan suffix in their filenames (e.g. feature_subplan1.md)
- Each subplan maintains its own checklist and context.yaml files
- Parent plans track overall progress while subplans focus on specific components

## References

<!-- SUBPLAN:hierarchical_plans_treeview_subplan -->
[Subplan: Tree Visualization Enhancement](hierarchical_plans_treeview_subplan.md)
<!-- END_SUBPLAN -->

[Checklist](hierarchical_plans_checklist.md)
