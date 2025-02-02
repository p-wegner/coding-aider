[Coding Aider Plan - Checklist]

## Core Dialog Implementation
- [ ] Create AiderPlanRefinementDialog class extending DialogWrapper
- [ ] Implement split view with current plan preview and refinement input
- [ ] Add markdown preview support using MarkdownJcefViewer
- [ ] Handle theme changes for preview panel
- [ ] Add refinement request text input area
- [ ] Create descriptive labels and help text

## Plan Viewer Integration
- [ ] Add "Refine Plan" action to PlanViewer toolbar
- [ ] Create icon and tooltip for refinement action
- [ ] Implement action update logic for enabled/disabled states
- [ ] Handle plan selection and validation

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