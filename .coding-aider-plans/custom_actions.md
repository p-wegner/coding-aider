# [Coding Aider Plan]

## Custom Actions Feature

### Overview
Implement a custom actions feature that allows users to create and manage simple custom user actions that execute user-defined prompts with preconfigured context files. This feature should be configurable similar to the test generation feature.

### Key Components

#### 1. Custom Action Configuration
- **CustomActionConfiguration**: Data class to define custom actions
  - Name: Identifies the custom action
  - Prompt Template: The AI prompt template to execute
  - Context Files: Reference files that provide context
  - Enabled/Disabled: Toggle for availability in the UI

#### 2. Default Custom Actions
- **DefaultCustomActions**: Provides predefined custom action configurations
  - Code Review: Review code for quality, bugs, performance, security
  - Add Documentation: Add comprehensive documentation to code
  - Refactor Code: Improve code structure and maintainability

#### 3. Dialog Components
- **CustomActionDialog**: Main dialog for selecting custom actions and providing additional instructions
- **CustomActionConfigDialog**: Dialog for creating/editing custom action configurations

#### 4. Prompt Generation
- **CustomActionPromptService**: Builds prompts for the AI model by combining:
  - Selected files information
  - Configured context files
  - Custom action's prompt template
  - User-provided additional instructions

#### 5. Settings Integration
- Extend **AiderProjectSettings** to store custom actions
- Extend **AiderProjectSettingsConfigurable** to manage custom actions in the UI

### Implementation Steps

1. ✅ Create CustomActionConfiguration data class
2. ✅ Create DefaultCustomActions with predefined actions
3. ✅ Create CustomActionPromptService for prompt building
4. ✅ Create CustomActionDialog for execution
5. ✅ Create CustomActionConfigDialog for configuration
6. ✅ Extend AiderProjectSettings to store custom actions
7. ✅ Extend AiderProjectSettingsConfigurable to manage custom actions
8. ⏳ Add menu actions to trigger custom actions
9. ⏳ Update documentation

### Integration Points
- **Settings System**: Custom actions stored in project settings
- **Command Execution**: Uses common command execution framework
- **File System**: Integrates with IDE's file selection and VFS
- **Context Management**: Similar to test generation context file handling

### Future Enhancements
- Import/export custom action configurations
- Sharing custom actions between projects
- Custom action templates marketplace
- Integration with external tools and APIs
