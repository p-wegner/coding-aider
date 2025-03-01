[Coding Aider Plan - Checklist]

## Core Dialog Implementation
- [x] Create AiderPlanRefinementDialog class extending DialogWrapper
- [x] Implement split view with current plan preview and refinement input
- [x] Add markdown preview support using MarkdownJcefViewer
- [x] Handle theme changes for preview panel
- [x] Add refinement request text input area
- [x] Create descriptive labels and help text

## Plan Viewer Integration
- [x] Add "Refine Plan" action to PlanViewer toolbar
- [x] Create icon and tooltip for refinement action
- [x] Implement action update logic for enabled/disabled states
- [x] Handle plan selection and validation

## Refinement Processing
- [ ] Create refinement prompt template in AiderPlanPromptService
- [ ] Add method to generate refinement-specific prompts
- [ ] Integrate with existing command execution flow
- [ ] Handle subplan creation in refinement responses

## UI/UX Enhancements
- [ ] Style dialog components for consistency
- [ ] Add proper spacing and margins
- [ ] Implement focus handling
- [ ] Add keyboard shortcuts
- [ ] Ensure proper component sizing
