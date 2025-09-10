# [Coding Aider Plan]
# CLI Interface Abstraction Refactoring Plan

## Overview

This plan outlines the refactoring required to make the coding-aider plugin support multiple CLI tools (Aider, Claude Code, Gemini CLI, Codex CLI, etc.) instead of being hardcoded to Aider. The goal is to create a flexible architecture that allows for CLI interchangeability while maintaining existing functionality.

## Current State Analysis

### Aider-Specific Dependencies Found

#### 1. Command Execution (High Priority)
- **Hardcoded arguments**: `--message` (`-m`) used throughout `AiderExecutionStrategy.kt:115-152`
- **Aider-specific flags**: `--yes`, `--edit-format`, `--lint-cmd`, `--auto-commits`, `--dirty-commits`
- **Mode-specific arguments**: Different argument handling for NORMAL, STRUCTURED, ARCHITECT modes
- **File arguments**: `--file` and `--read` patterns for file inclusion

#### 2. Settings Structure (High Priority)
- **87 Aider-specific settings** in `AiderSettings.kt` including:
  - Docker configuration (`useDockerAider`, `dockerImage`, `mountAiderConfInDocker`)
  - Model selection (`llm`, `webCrawlLlm`, `documentationLlm`)
  - Execution preferences (`useYesFlag`, `additionalArgs`, `lintCmd`)
  - Aider-specific features (`editFormat`, `deactivateRepoMap`, `includeChangeContext`)
  - Sidecar mode (`useSidecarMode`, `sidecarModeVerbose`)

#### 3. Command Data Structure (Medium Priority)
- **Mixed concerns** in `CommandData.kt`: Generic execution data mixed with Aider-specific parameters
- **AiderMode enum**: Tightly coupled to Aider's execution modes
- **Hardcoded argument patterns**: Direct references to Aider argument structure

#### 4. Model Selection System (Medium Priority)
- **Complex model handling**: `ModelConfiguration.kt` with Aider-specific model mappings
- **Custom provider system**: `CustomLlmProviderService` with Aider-centric configuration
- **API key management**: `ApiKeyChecker` with hardcoded model-to-API-key mappings
- **Provider types**: `LlmProviderType` enum with Aider-specific prefixes and configurations


## Target Architecture

### 1. CLI Abstraction Layer

#### Core Interfaces
```kotlin
interface CliInterface {
    fun buildCommand(commandData: GenericCommandData): List<String>
    fun prepareEnvironment(processBuilder: ProcessBuilder, commandData: GenericCommandData)
    fun cleanupAfterExecution()
    fun supportsFeature(feature: CliFeature): Boolean
    fun getArgumentMappings(): Map<GenericArgument, CliArgument>
}

interface CliModelHandler {
    fun resolveModel(modelName: String): String
    fun getRequiredEnvironmentVariables(modelName: String): Map<String, String>
    fun supportsModel(modelName: String): Boolean
}

interface CliArgumentMapper {
    fun mapGenericArgument(genericArg: GenericArgument, value: String): CliArgument
    fun getPromptArgument(): CliArgument
    fun getFileArgument(isReadOnly: Boolean): CliArgument
}
```

#### Generic Argument Types
```kotlin
enum class GenericArgument {
    PROMPT,
    MODEL,
    FILE,
    READ_ONLY_FILE,
    YES_FLAG,
    EDIT_FORMAT,
    LINT_COMMAND,
    AUTO_COMMIT,
    DIRTY_COMMIT,
    // ... other common arguments
}
```

### 2. CLI Implementations

#### AiderCli Implementation
```kotlin
class AiderCli : CliInterface {
    override fun buildCommand(commandData: GenericCommandData): List<String> {
        // Current Aider-specific logic moved here
        return listOf("aider") + mapArguments(commandData)
    }
    
    override fun getArgumentMappings(): Map<GenericArgument, CliArgument> {
        return mapOf(
            GenericArgument.PROMPT to CliArgument("-m"),
            GenericArgument.MODEL to CliArgument("--model"),
            GenericArgument.FILE to CliArgument("--file"),
            GenericArgument.READ_ONLY_FILE to CliArgument("--read"),
            // ... other mappings
        )
    }
}
```

#### ClaudeCodeCli Implementation
```kotlin
class ClaudeCodeCli : CliInterface {
    override fun buildCommand(commandData: GenericCommandData): List<String> {
        return listOf("claude") + mapArguments(commandData)
    }
    
    override fun getArgumentMappings(): Map<GenericArgument, CliArgument> {
        return mapOf(
            GenericArgument.PROMPT to CliArgument("-p"),  // Claude Code uses -p for prompt
            GenericArgument.MODEL to CliArgument("--model"),
            // Claude Code may not support all Aider features
        )
    }
}
```

### 3. Command Structure Refactoring

#### Generic Command Data
```kotlin
data class GenericCommandData(
    val prompt: String,
    val model: String,
    val files: List<FileData>,
    val options: GenericCommandOptions,
    val cliMode: CliMode,
    val projectPath: String
)

data class GenericCommandOptions(
    val useYesFlag: Boolean,
    val editFormat: String?,
    val lintCommand: String?,
    val autoCommit: Boolean?,
    val dirtyCommit: Boolean?,
    val additionalArgs: Map<String, String> = emptyMap()
)
```

#### CLI-Specific Command Data
```kotlin
data class CliSpecificCommand(
    val executable: String,
    val arguments: List<String>,
    val environment: Map<String, String>,
    val workingDirectory: String
)
```

### 4. Settings Reorganization

#### Generic Settings
```kotlin
@Service
class GenericCliSettings : PersistentStateComponent<GenericCliSettings.State> {
    data class State(
        var selectedCli: String = "aider",
        var commonLlmProviders: List<LlmProviderConfig> = emptyList(),
        var defaultModel: String = "",
        var commonExecutionOptions: CommonExecutionOptions = CommonExecutionOptions()
    )
}

data class CommonExecutionOptions(
    val useDocker: Boolean = false,
    val dockerImage: String = "",
    val verboseLogging: Boolean = false
)
```

#### CLI-Specific Settings
```kotlin
@Service
class AiderSpecificSettings : PersistentStateComponent<AiderSpecificSettings.State> {
    data class State(
        var aiderExecutablePath: String = "aider",
        var editFormat: String = "",
        var includeChangeContext: Boolean = false,
        var useSidecarMode: Boolean = false,
        // ... other Aider-specific settings
    )
}

@Service
class ClaudeCodeSpecificSettings : PersistentStateComponent<ClaudeCodeSpecificSettings.State> {
    data class State(
        var claudeExecutablePath: String = "claude",
        var maxTokens: Int = 4000,
        var temperature: Double = 0.7,
        // ... other Claude Code-specific settings
    )
}
```

## Refactoring Phases

### Phase 1: Core Abstraction (Week 1-2)

#### Tasks:
1. **Create CLI interface hierarchy**
   - Define `CliInterface`, `CliModelHandler`, `CliArgumentMapper`
   - Create `GenericArgument` and `CliArgument` types
   - Implement base classes for CLI-specific functionality

2. **Extract generic command data structures**
   - Create `GenericCommandData` and `GenericCommandOptions`
   - Refactor `CommandData` to use generic structures
   - Create `CliSpecificCommand` for runtime execution

3. **Refactor settings management**
   - Create `GenericCliSettings` for common settings
   - Extract CLI-specific settings into separate classes
   - Implement `SettingsFactory` pattern for CLI selection

4. **Build argument mapping system**
   - Create `CliArgumentMapper` implementations
   - Build argument validation and conversion utilities
   - Create CLI feature detection system

#### Deliverables:
- CLI interface definitions
- Generic command data structures
- Settings factory implementation
- Argument mapping system
- Unit tests for core abstractions

### Phase 2: Implementation Refactoring (Week 3-4)

#### Tasks:
1. **Refactor execution strategies**
   - Update `AiderExecutionStrategy` to implement `CliInterface`
   - Create `ClaudeCodeExecutionStrategy`
   - Refactor `CommandExecutor` to use CLI interfaces
   - Update Docker and native execution strategies

2. **Update command processing pipeline**
   - Refactor `CommandDataCollector` to work with generic structures
   - Update `CommandExecutor` to use CLI interfaces
   - Create CLI-specific command builders
   - Update environment preparation and cleanup

3. **Migrate settings management**
   - Update settings panels to use new structure
   - Create CLI selection UI components
   - Migrate existing settings to new structure
   - Update settings change listeners

4. **Create CLI-specific argument builders**
   - Implement argument builders for each CLI
   - Create argument validation and error handling
   - Build CLI-specific help and documentation
   - Create CLI feature compatibility matrix

#### Deliverables:
- Refactored execution strategies
- Updated command processing pipeline
- New settings management system
- CLI-specific argument builders
- Integration tests for refactored components

### Phase 3: UI and Actions Update (Week 5-6)

#### Tasks:
1. **Make dialog components CLI-agnostic**
   - Update `AiderInputDialog` to use generic structures
   - Create `CliOptionsPanel` for CLI-specific options
   - Refactor `AiderOptionsPanel` to be generic
   - Update completion providers to work with multiple CLIs

2. **Update actions to use CLI interfaces**
   - Refactor `AiderAction` and related actions
   - Create CLI-specific action variants
   - Update action visibility and enablement logic
   - Create CLI selection menu items

3. **Create CLI selection UI**
   - Add CLI selection to settings dialog
   - Create CLI configuration panels
   - Update tool windows to show CLI information
   - Create CLI-specific help and documentation

4. **Update settings panels**
   - Refactor `AiderSettingsConfigurable` to use new structure
   - Create CLI-specific settings panels
   - Update settings validation and persistence
   - Create settings migration utilities

#### Deliverables:
- CLI-agnostic dialog components
- Updated action system
- CLI selection UI
- Refactored settings panels
- UI tests for new components

### Phase 4: Feature Adaptation (Week 7-8)

#### Tasks:
1. **Create CLI-specific feature implementations**
   - Implement Aider-specific features in `AiderCli`
   - Create Claude Code-specific features
   - Build feature compatibility matrix
   - Create feature toggles and settings

## Features to Remove or Modify

### High-Cost Features for Interchangeability

#### 1. Plugin-based Edits (Remove or Aider-only)
- **Current**: `PluginBasedEditsService` uses Aider-specific search/replace format
- **Issue**: Other CLIs may not support this format
- **Decision**: Make Aider-only or remove entirely
- **Impact**: Users will lose in-IDE editing capabilities for other CLIs

#### 2. Auto-commit Integration (Refactor)
- **Current**: `AutoCommitService` coupled with Aider's commit detection
- **Issue**: Different CLIs may have different commit behaviors
- **Decision**: Make CLI-agnostic with CLI-specific implementations
- **Impact**: May need to simplify auto-commit features

### Conditional Features

#### 1. Docker Integration (Keep but refactor)
- **Current**: Aider-specific Docker configuration
- **Decision**: Keep but make CLI-configurable
- **Impact**: Maintains containerized execution support

#### 2. Shell Mode (Keep but adapt)
- **Current**: Aider-specific shell integration
- **Decision**: Keep generic concept with CLI-specific implementation
- **Impact**: Maintains shell execution capability

#### 3. Model Reasoning Effort (CLI-specific)
- **Current**: Aider-specific reasoning effort configuration
- **Decision**: Make CLI-specific feature
- **Impact**: Some CLIs may not support this feature

#### 4. Edit Format Selection (CLI-specific)
- **Current**: Aider-specific edit format options
- **Decision**: Make CLI-specific with fallbacks
- **Impact**: Different edit format support per CLI

## Implementation Strategy

### Step-by-Step Approach

1. **Week 1**: Create core abstractions and interfaces
2. **Week 2**: Implement generic command data and settings
3. **Week 3**: Refactor execution strategies
4. **Week 4**: Update command processing and settings
5. **Week 5**: Refactor UI components
6. **Week 6**: Update actions and create CLI selection
7. **Week 7**: Adapt features and create CLI-specific implementations
8. **Week 8**: Testing, documentation, and final integration

### Risk Mitigation

1. **Feature Loss**: Clearly communicate which features will be lost or modified
2. **Breaking Changes**: Provide migration guide and settings migration
3. **Performance**: Benchmark performance impact of abstraction layer
4. **Compatibility**: Ensure existing Aider functionality is preserved