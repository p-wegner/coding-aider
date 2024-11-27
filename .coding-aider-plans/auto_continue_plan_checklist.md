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

- [x] Add autoclose continuation
    - [x] Modify autoclose timer to check for plan continuation
    - [x] Add continuation logic to disposal process
    - [x] Handle edge cases during autoclose

- [x] Enhance UI behavior
    - [x] Improve button state management during continuation
    - [x] Add proper error handling for continuation failures
    - [x] Add progress indication during continuation

- [ ]  cleanup
    - [ ] Verify proper resource cleanup
    - [ ] Implement needed error handling paths
