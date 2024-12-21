[Coding Aider Plan - Checklist]

# Hierarchical Plans Implementation Checklist

- [x] Data Structure Updates
    - [x] Add parent/child relationship fields to AiderPlan class
    - [x] Implement methods for traversing plan hierarchies
        - Added findCommonAncestor companion method
        - Added findSiblingPlans method
        - Added getAncestors method
        - Added isDescendantOf method
        - Added getNextUncompletedPlan method
    - [x] Add support for plan references in markdown files
    - [x] Update plan loading to handle hierarchical relationships

- [ ] UI Enhancements
    - [ ] Modify PlanViewer to show hierarchical structure
    - [ ] Add indentation or tree view for nested plans
    - [ ] Update tooltips to show plan hierarchy information
    - [ ] Add visual indicators for parent/child relationships
    - [ ] Complete tree visualization implementation

- [x] Plan Execution Logic
    - [x] Implement logic to find next executable plan in hierarchy
    - [x] Handle completion of child plans
    - [x] Add support for continuing parent plans
    - [x] Update progress tracking for hierarchical structures

- [x] Plan Creation and Management
    - [x] Add support for creating nested plans from single prompt
    - [x] Implement plan refinement into subplans
    - [x] Update file naming conventions for nested plans
    - [x] Ensure proper context.yaml handling for nested plans

- [x] Plan Refinement
    - [x] Add an action to refine an existing plan
        - [x] The action should be triggered by a button in the PlanViewer
        - [x] A dialog should be displayed with an input field for a user prompt what to refine in the plan
        - [x] Depending on the user input, the plan should be refined and possibly extended to a hierarchical plan
            - [x] Added appropriate prompt handling in AiderPlanPromptService
            - [x] Implemented plan refinement dialog
            - [x] Added support for creating subplans based on user input
