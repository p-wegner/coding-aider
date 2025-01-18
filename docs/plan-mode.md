# Structured Mode and Plans

## Overview

Structured Mode in Coding-Aider addresses a common limitation of AI coding assistants: the lack of persistent context between interactions. Unlike chat-based plugins that maintain conversation history, Coding-Aider uses a plan-based approach to track progress and maintain context across multiple coding sessions.

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
   - Aider updates checklists as tasks are completed
   - Leave the message empty to continue with the current plan

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
   - `authentication_login.md`
   - `authentication_registration.md`

2. **Implementation**
   - Open relevant files in IDE
   - Use Aider commands with empty message to continue plan
   - Or provide specific instructions within plan context
   - Watch checklist items get marked as completed

3. **Review Progress**
   - Check tool window for plan status
   - Review remaining tasks in checklists
   - Navigate between main plan and subplans

## Best Practices

1. **Plan Organization**
   - Use clear, descriptive plan names
   - Break complex features into subplans
   - Keep tasks atomic and measurable

2. **Context Management**
   - Include all relevant files in context.yaml
   - Update context as new files are created
   - Use read-only flag for reference files

3. **Implementation**
   - Work on one subplan at a time
   - Let Aider manage checklist updates
   - Review and adjust plans as needed

## Tips

- Use empty messages to continue current plan
- Check tool window for plan progress
- Review generated plans before implementation
- Adjust context files as needed
- Create subplans for independent features
