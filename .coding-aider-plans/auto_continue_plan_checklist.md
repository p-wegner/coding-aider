[Coding Aider Plan - Checklist]

# Implementation Checklist

- [x] Add "Close and Continue" button to MarkdownDialog
    - [x] Create button with proper styling and mnemonics
    - [x] Position button in button panel
    - [x] Add visibility logic for structured mode

- [x] Implement continuation logic
    - [x] Create ContinuePlanService
    - [x] Add method to check for open checklist items
    - [x] Implement plan continuation trigger

- [ ] Add autoclose continuation
    - [ ] Modify autoclose timer to check for plan continuation
    - [ ] Add continuation logic to disposal process
    - [ ] Handle edge cases during autoclose

- [ ] Enhance UI behavior
    - [ ] Improve button state management during continuation
    - [ ] Add proper error handling for continuation failures
    - [ ] Add progress indication during continuation

- [ ] Testing and cleanup
    - [ ] Test all continuation scenarios
    - [ ] Verify proper resource cleanup
    - [ ] Test error handling paths
