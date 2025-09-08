# Custom Slash Commands in Claude Code

**Custom slash commands are Markdown files that contain prompts for Claude Code.** They extend Claude Code with reusable workflows, automation, and specialized tasks. The entire content of the Markdown file serves as a prompt that Claude will execute with the provided arguments.

## Key Concept: Commands as Prompts

Unlike traditional command-line tools, slash commands in Claude Code are **prompts**, not just scripts. The entire Markdown content is sent to Claude as a prompt, allowing for:

- Natural language instructions and context
- Complex reasoning and decision-making
- Flexible tool usage (Bash, Read, Write, Edit, etc.)
- Dynamic responses based on arguments and context
- Multi-step workflows with AI-driven decision making

## Directory Structure

### Project-Specific Commands
- Location: `.claude/commands/`
- Scope: Available only in the current project
- Use case: Project-specific build, test, and deployment workflows

### Personal Commands
- Location: `~/.claude/commands/`
- Scope: Available across all projects
- Use case: Personal utilities and frequently used workflows

## Command Structure

A custom slash command is a Markdown file with frontmatter metadata followed by a prompt:

```markdown
---
description: "Brief description of what the command does"
argument-hint: "[optional-arguments]"
allowed-tools: [Bash, Read, Write, Edit, etc.]
model: "claude-3-5-sonnet-20241022"
---

# Command Title

This is a prompt for Claude. You can write natural language instructions,
provide context, and specify what you want Claude to do with the arguments.

The arguments provided are: `$ARGUMENTS`
Positional arguments: `$1`, `$2`, etc.

Claude will read this entire prompt and execute it using the allowed tools.
```

## Frontmatter Fields

### Required Fields
- `description`: Brief description of the command's purpose

### Optional Fields
- `argument-hint`: Hint about expected arguments (shown in UI)
- `allowed-tools`: List of tools the command can use (e.g., [Bash, Read, Write])
- `model`: Specific Claude model to use for the command

## Argument Handling

### All Arguments
- `$ARGUMENTS`: Contains all arguments passed to the command
- Example: `/build test --info` → `$ARGUMENTS` = "test --info"

### Positional Arguments
- `$1`, `$2`, `$3`, etc.: Access specific positional arguments
- Example: `/deploy production main` → `$1` = "production", `$2` = "main"

## Command Execution

Since slash commands are prompts, you can mix natural language with tool usage:

### Bash Commands
When you need to execute shell commands, use the Bash tool:

```markdown
Please run the Gradle build task: `$ARGUMENTS`

```bash
./gradlew $ARGUMENTS
```
```

### File References
When you need to reference files in your commands:

```markdown
Please read the build configuration file and analyze it:

@src/main/kotlin/BuildConfig.kt

Then provide a summary of the configuration.
```

### Natural Language Instructions
The power of slash commands comes from natural language:

```markdown
I need you to help me debug a test failure. 

Please:
1. Run the tests for the class `$1`
2. If tests fail, analyze the failure
3. Look at the source code for that class
4. Suggest fixes for the issues

Use the Read and Bash tools as needed.
```

## Examples

### Build Command (Simple)
```markdown
---
description: "Run Gradle build tasks"
argument-hint: "[task-name]"
allowed-tools: [Bash]
---

# Build Plugin

Please run the Gradle build task: `$ARGUMENTS`

```bash
./gradlew $ARGUMENTS
```
```

### Code Review Command (Natural Language)
```markdown
---
description: "Review code changes and provide feedback"
argument-hint: "[file-path]"
allowed-tools: [Read, Bash, Glob]
---

# Code Review

Please review the code changes for: `$1`

Steps to follow:
1. Check git status to see what files have changed
2. Read the specified file if provided
3. Analyze the code quality, potential issues, and best practices
4. Provide constructive feedback

```bash
git status
```

If a specific file was mentioned, please read it:
`$1`
```

### Debug Command (Complex Reasoning)
```markdown
---
description: "Debug test failures and suggest fixes"
argument-hint: "[test-class]"
allowed-tools: [Bash, Read, Write, Edit, Glob]
---

# Debug Test Failures

I need help debugging test failures. Please help me:

1. Run tests for the class: `$1`
2. Analyze any failures
3. Examine the source code
4. Identify the root cause
5. Suggest and implement fixes

Start by running the tests:

```bash
./gradlew test --tests "$1"
```

Then analyze the results and examine the corresponding source files.
```

### Documentation Generator (AI-Powered)
```markdown
---
description: "Generate documentation for code files"
argument-hint: "[file-or-directory]"
allowed-tools: [Read, Glob, Write]
---

# Generate Documentation

Please generate comprehensive documentation for: `$1`

If it's a directory, find all relevant source files and generate documentation for each.
If it's a specific file, generate detailed documentation for that file.

For each file, please:
1. Read and analyze the code
2. Understand its purpose and functionality
3. Generate markdown documentation
4. Save the documentation in a docs/ subdirectory

The documentation should include:
- Purpose and overview
- Function/method descriptions
- Parameters and return values
- Usage examples
- Dependencies and requirements
```

### Refactoring Assistant (Intelligent Analysis)
```markdown
---
description: "Analyze code and suggest refactoring improvements"
argument-hint: "[file-path]"
allowed-tools: [Read, Write, Edit, Glob]
---

# Refactoring Assistant

Please analyze the code in: `$1` and suggest refactoring improvements.

Your analysis should include:
1. Code quality assessment
2. Identification of code smells
3. Performance optimization opportunities
4. Best practices violations
5. Specific refactoring suggestions

Please:
1. Read the file thoroughly
2. Identify areas for improvement
3. Provide specific, actionable suggestions
4. If appropriate, implement the improvements

Focus on making the code more maintainable, readable, and efficient.
```

## Usage

1. Create a command file in `.claude/commands/` or `~/.claude/commands/`
2. Use the command with `/command-name [arguments]`
3. Claude Code will execute the Markdown file with the provided arguments

## Best Practices

1. **Descriptive Names**: Use clear, concise command names
2. **Argument Hints**: Provide helpful hints for expected arguments
3. **Tool Permissions**: Only allow necessary tools for security
4. **Error Handling**: Include error messages and helpful output
5. **Documentation**: Include clear descriptions and usage examples

## Advanced Features

### Conditional Logic
Use bash conditionals for complex workflows:

```markdown
```bash
if [ "$1" = "production" ]; then
  ./gradlew build -Penv=prod
else
  ./gradlew build -Penv=dev
fi
```
```

### Multiple Commands
Chain commands for complex workflows:

```markdown
```bash
./gradlew clean
./gradlew build
./gradlew test
```
```

## Troubleshooting

1. **Command Not Found**: Verify file is in correct directory (`.claude/commands/`)
2. **Permission Issues**: Ensure bash commands are executable
3. **Argument Issues**: Check argument syntax and spacing
4. **Tool Restrictions**: Verify required tools are in `allowed-tools`

## Related Resources

- [Claude Code Slash Commands Documentation](https://docs.anthropic.com/en/docs/claude-code/slash-commands)
- [Gradle Build Commands](https://docs.gradle.org/current/userguide/command_line_interface.html)
- [IntelliJ Plugin Development](https://plugins.jetbrains.com/docs/intellij/)