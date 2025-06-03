# [Coding Aider Plan]

## Custom Actions Feature

### Overview
Implement a system for managing and executing custom user-defined actions that can execute predefined prompts with configurable context files. This feature should be similar to the existing test generation functionality but more flexible for general-purpose tasks.

### Core Components

#### 1. Data Models
- `CustomActionConfiguration`: Configuration for custom actions including name, prompt template, enabled state, and context files
- `DefaultCustomActions`: Provides default custom actions (Code Review, Add Documentation, Refactor Code)

#### 2. Services
- `CustomActionPromptService`: Builds prompts for custom actions, combining templates with selected files and context

#### 3. UI Components
- `CustomActionDialog`: Main dialog for selecting and executing custom actions
- `CustomActionTypeDialog`: Dialog for creating/editing custom action configurations

#### 4. Settings Integration
- Extend `AiderProjectSettings` to store custom action configurations
- Extend `AiderProjectSettingsConfigurable` to provide UI for managing custom actions

### Features
- Create, edit, delete, and copy custom actions
- Enable/disable individual actions
- Configure context files for each action (similar to test types)
- Execute actions with additional user instructions
- Store configurations persistently per project

### Implementation Status
- ✅ Core data models implemented
- ✅ Service layer implemented
- ✅ UI dialogs implemented
- ✅ Settings integration completed
- ⏳ Action integration (menu items, shortcuts) - pending
- ⏳ Documentation updates - pending

### Next Steps
1. Add menu actions to trigger custom actions
2. Update documentation
3. Add keyboard shortcuts
4. Consider adding action groups/categories
