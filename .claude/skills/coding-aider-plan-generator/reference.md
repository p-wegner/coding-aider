# Coding-Aider Plan System Reference

This document provides detailed reference information about the coding-aider plugin's plan system to help generate accurate and consistent plans.

## Plan File Formats

### Multi-File Format (Traditional)
The standard format used by the coding-aider plugin:

```
.coding-aider-plans/
├── feature-name.md
├── feature-name_checklist.md
└── feature-name_context.yaml
```

### Single-File Format (Simplified)
Alternative format that embeds checklist and context in the main file:

```
.coding-aider-plans/
└── feature-name.md
```

## Plan File Structure Details

### Main Plan File (feature-name.md)

#### Required Header
```markdown
# [Coding Aider Plan]
```

#### Standard Sections
1. **Plan Title** - H1 header with descriptive name
2. **Overview** - High-level feature description
3. **Problem Description** - Current issues and requirements
4. **Goals** - Numbered list of specific objectives
5. **Additional Notes and Constraints** - Technical constraints, dependencies
6. **References** - Related files and documentation

#### Subplan Support (Optional)
```markdown
<!-- SUBPLAN:subfeature-name -->
[Subplan: Subfeature Name](subfeature-name.md)
<!-- END_SUBPLAN -->
```

### Checklist File (feature-name_checklist.md)

#### Required Header
```markdown
# [Coding Aider Plan - Checklist]
```

#### Format Requirements
- Use GitHub-flavored markdown checkboxes: `- [ ]`
- Each item should represent an atomic, completable task
- Order: Analysis → Setup → Implementation → Testing → Documentation
- Typical count: 8-15 items depending on complexity

### Context File (feature-name_context.yaml)

#### YAML Structure
```yaml
---
files:
  - path: "relative/path/to/file"
    readOnly: false
  - path: "relative/path/to/readonly-file"
    readOnly: true
```

#### File Selection Guidelines
- Include all files that will need modification
- Mark configuration files as read-only
- Include test files, build files, documentation
- Use relative paths from project root

## Plan Generation Templates

### New Plan Template Structure
The plugin uses these sections when generating new plans:

1. **Feature Analysis**: Understanding user requirements
2. **Problem Identification**: Current state and issues
3. **Goal Definition**: Specific, measurable outcomes
4. **Implementation Planning**: Breaking down into tasks
5. **Context Identification**: Relevant files and dependencies

### Checklist Generation Patterns
The plugin follows these patterns for checklist items:

#### Analysis Tasks
- `[ ] Research current implementation`
- `[ ] Analyze requirements and constraints`
- `[ ] Review similar features in codebase`

#### Setup Tasks
- `[ ] Create necessary directory structure`
- `[ ] Set up configuration files`
- `[ ] Prepare test environment`

#### Implementation Tasks
- `[ ] Implement core functionality`
- `[ ] Add error handling`
- `[ ] Integrate with existing systems`
- `[ ] Update related components`

#### Testing Tasks
- `[ ] Write unit tests`
- `[ ] Write integration tests`
- `[ ] Perform manual testing`
- `[ ] Validate edge cases`

#### Documentation Tasks
- `[ ] Update API documentation`
- `[ ] Add code comments`
- `[ ] Update README files`

## Context File Discovery

### Common File Patterns to Include
- **Source Files**: `src/main/kotlin/**/*.kt`, `src/main/java/**/*.java`
- **Test Files**: `src/test/kotlin/**/*.kt`, `src/test/java/**/*.java`
- **Build Files**: `build.gradle.kts`, `pom.xml`, `package.json`
- **Configuration**: `application.yml`, `settings.gradle.kts`
- **Documentation**: `README.md`, `docs/**/*.md`

### File Permission Guidelines
#### Read-Only Files (readOnly: true)
- Build configuration files
- External library dependencies
- Generated files
- Third-party configurations

#### Writable Files (readOnly: false)
- Main source code files
- Test files
- Documentation
- Project-specific configuration

## Plan Naming Conventions

### Plan Name Rules
- Use kebab-case (lowercase with hyphens)
- Maximum 50 characters
- Descriptive but concise
- Include main feature or component name

### Examples
- `user-authentication`
- `payment-processing-refactor`
- `memory-leak-fix`
- `api-rate-limiting`
- `database-migration`

## Plan Lifecycle

### Creation
1. User provides feature description
2. Plugin analyzes current codebase
3. Plan files are generated in `.coding-aider-plans/`
4. Context files are populated with relevant files

### Execution
1. Developer works through checklist items
2. Files are modified according to plan
3. Context files are updated as needed
4. Progress is tracked via checkbox completion

### Completion
1. All checklist items are completed
2. Plan is moved to `.coding-aider-plans-finished/`
3. Summary of implementation is generated

### Continuation
1. Unfinished plans can be resumed
2. Context and progress are preserved
3. Additional tasks can be added if needed

## Integration with Aider

### File Context Management
The context.yaml file provides file context to aider commands:
- Files are automatically included in aider sessions
- Read-only files are provided as reference
- Writable files are available for modification

### Plan-Aider Workflow
1. Plan defines implementation strategy
2. Context files prepare aider environment
3. Aider executes implementation based on plan
4. Checklist tracks progress through plan items

## Error Handling

### Common Issues
- **Missing Files**: Context files reference non-existent files
- **Permission Issues**: Unable to create plan directory
- **Invalid YAML**: Malformed context.yaml structure
- **Naming Conflicts**: Plan name already exists

### Resolution Strategies
- Validate file paths before creating context
- Check directory permissions and create if needed
- Use YAML validation for context files
- Generate unique plan names with suffixes

## Best Practices

### Plan Quality
- Make goals specific and measurable
- Break implementation into atomic tasks
- Include comprehensive testing steps
- Maintain accurate file context

### Maintenance
- Update context files as codebase changes
- Mark completed checklist items
- Add new tasks if scope expands
- Archive completed plans properly

### Collaboration
- Use clear, descriptive task names
- Include relevant documentation links
- Provide sufficient context for team members
- Follow consistent naming conventions