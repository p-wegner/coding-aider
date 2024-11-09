[Coding Aider Plan]

# Display Available Plans in Toolwindow

This feature will enhance the Persistent Files Toolwindow by adding a dedicated section to display all available Aider plans. The implementation will:

1. Add a new section below the existing Persistent Files list
2. Show plans with their completion status
3. Calculate and display the number of open checklist items
4. Allow opening plans by double-clicking
5. Provide visual feedback on plan status

The plans will be displayed in a list format similar to the persistent files, but with additional status information. Each plan entry will show:
- Plan name (from filename)
- Completion status (✓ for complete, ⋯ for in progress)
- Number of open checklist items
- Execute button to continue working on the plan

See [display_plans_in_toolwindow_checklist](display_plans_in_toolwindow_checklist.md) for implementation steps.

Technical Details:
- Uses existing AiderPlanService to load and parse plans
- Implements custom list cell renderer for plans
- Adds UI controls for plan management
- Integrates with existing toolwindow infrastructure