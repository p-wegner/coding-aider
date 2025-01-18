# Structured Mode and Plans

## Overview

Structured Mode in Coding-Aider addresses a common limitation of AI coding assistants: the lack of persistent context between interactions. Unlike chat-based plugins that maintain conversation history, Coding-Aider uses a plan-based approach to track progress and maintain context across multiple coding sessions. 
This ensures the LLM is less confused by previous actions and potential error fixing loops and can focus on individual tasks, while having a broader view of the general goal for a given feature. 
This reduces the need to manually (re-)prompt the LLM for longer and more complex tasks and gives users the ability to intervene when needed.

## Key Benefits

1. **Progress Tracking**: Plans break down complex features into manageable tasks with checkboxes
2. **Context Persistence**: Each plan maintains its own context files, ensuring relevant code is always available
3. **Hierarchical Organization**: Support for main plans and subplans helps manage complex features
4. **Automated Updates**: Checklist items are automatically marked as completed as you implement features
5. **Visual Progress**: The tool window shows plan progress and remaining tasks

## How It Works

1. **Plan Creation**
   - Enable Structured Mode in the Aider Command dialog
   - Describe your feature or coding task
   - Aider creates three files in `.coding-aider-plans`:
     - `feature_name.md`: Main plan with overview, goals, and implementation details
     - `feature_name_checklist.md`: Granular implementation steps
     - `feature_name_context.yaml`: List of relevant implementation files

2. **Implementation**
   - Plans appear in the Coding-Aider tool window
   - Select files and use Aider commands as usual
   - The plan's context files are automatically included
   - Aider updates checklists as tasks are completed (implementation order and task batching may be liberally chosen by the LLM)
   - Leave the message empty to continue with a plan in context

3. **Complex Features**
   - Large features are broken into subplans
   - Each subplan has its own checklist and context
   - Main plan tracks overall progress
   - Implement subplans independently or sequentially

## Example Workflow

1. **Starting a New Feature**
   ```
   [Structured Mode ON]
   Message: "Add user authentication with login and registration"
   ```
   Aider creates:
   - `authentication.md`
   - `authentication_checklist.md`
   - `authentication_context.yaml`
   And potentially subplans:
   - `authentication_login.md` (with checklist and context files)
   - `authentication_registration.md` (with checklist and context files)

2. **Implementation**
   - Open relevant files in IDE
   - Use Aider commands with empty message to continue plan (the llm will decide what to do next and may (not) check checklist items)
   - Or provide specific instructions within plan context
   - Watch checklist items get marked as completed

3. **Review Progress**
   - Check tool window for plan status
   - Review remaining tasks in checklists
   - Navigate between main plan and subplans

## Tips

- Use empty messages with plan files in file context or the dedicated buttons and actions to continue implementing a plan
- Review generated plans before or during implementation
- Make manual/plugin assisted adjustments to the plan files and checklists as needed, consider removing or adding checklist items as needed
- Testing and documentation steps are likely to yield undesired results since those tend to be unspecific and generic, you may want to 
  - provide guidelines for both or 
  - specify the exact steps to be taken (how and what to document, what types of tests to write, what libraries to use, ...)
  - implement tests manually or without using structured mode
  - skip/remove those steps from the plan
  - ideally provide example or template code for documentation and tests to ensure consistency
- Some checklist items may be suitable for manual work, e.g. evaluation of libraries or frameworks, manual testing 
- Check tool window for plan progress and additional actions
   - Archive plan => move all plan files of the selected plan to the .coding-aider-plans-finished folder
   - Continue plan => allows you to select and continue an unfinished plan within the Coding Aider plugin
   - Edit context => edit context files for this plan
   - New plan => create a new plan
   - Refine plan => refine and extend the selected plan, this may create subplans
- There is a setting to automatically continue with a plan when there are open checklist items 
  - => somewhat agent-like behavior with missing feedback loops that will likely need to be done manually/assisted once the plan is finished (compile errors, failing tests)
  - manual testing will likely reveal errors or missing functionality => uncheck and/or refine the related checklist items and continue with the plan

