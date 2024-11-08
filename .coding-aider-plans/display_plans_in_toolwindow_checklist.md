[Coding Aider Plan - Checklist]

See [display_plans_in_toolwindow.md](display_plans_in_toolwindow.md) for the full plan.

Implementation Checklist:

- [X] Create data structures for plan display
    - [X] Add ChecklistItem data class
    - [X] Add AiderPlan data class with completion status calculation

- [ ] Enhance PersistentFilesToolWindow
    - [X] Add plans list model and JBList component
    - [X] Create custom cell renderer for plans
    - [X] Add plans section to toolwindow UI
    - [ ] Implement double-click to open plan files
    - [ ] Add execute button functionality
    - [X] Add refresh button for plans list

- [X] Implement plan loading and status calculation
    - [X] Load plans from .coding-aider-plans directory
    - [X] Parse plan markdown files
    - [ ] Parse and track checklist completion status correctly 
    - [ ] (show number of open and completed items)
    - [X] Calculate open items count

- [ ] Add visual feedback
    - [ ] Show completion status icons
    - [ ] Display open items count
    - [ ] Add tooltips with file paths
    - [ ] Style execute button
