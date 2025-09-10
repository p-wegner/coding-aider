# CLI Interface Abstraction Refactoring Checklist

## Phase 1: Core Abstraction (Week 1-2)

### 1. Create CLI interface hierarchy
- [ ] Define `CliInterface` base interface with core methods
- [ ] Define `CliModelHandler` interface for model resolution
- [ ] Define `CliArgumentMapper` interface for argument mapping
- [ ] Create `GenericArgument` enum with common CLI arguments
- [ ] Create `CliArgument` data class for CLI-specific arguments
- [ ] Create `CliFeature` enum for feature detection
- [ ] Implement base classes for CLI-specific functionality

### 2. Extract generic command data structures
- [ ] Create `GenericCommandData` class to replace Aider-specific CommandData
- [ ] Create `GenericCommandOptions` class for common options
- [ ] Create `CliMode` enum to replace AiderMode
- [ ] Create `CliSpecificCommand` data class for runtime execution
- [ ] Refactor existing CommandData to use generic structures
- [ ] Update FileData to be CLI-agnostic

### 3. Refactor settings management
- [ ] Create `GenericCliSettings` service for common settings
- [ ] Create `CommonExecutionOptions` data class
- [ ] Extract CLI-specific settings from AiderSettings into `AiderSpecificSettings`
- [ ] Create `ClaudeCodeSpecificSettings` service
- [ ] Implement `SettingsFactory` pattern for CLI selection
- [ ] Create settings migration utilities for existing configurations

### 4. Build argument mapping system
- [ ] Create `CliArgumentMapper` implementations for each CLI
- [ ] Build argument validation utilities
- [ ] Build argument conversion utilities
- [ ] Create CLI feature detection system
- [ ] Create CLI feature compatibility matrix

## Phase 2: Implementation Refactoring (Week 3-4)

### 1. Refactor execution strategies
- [ ] Update `AiderExecutionStrategy` to implement `CliInterface`
- [ ] Create `ClaudeCodeExecutionStrategy` implementing `CliInterface`
- [ ] Create `GeminiCliExecutionStrategy` implementing `CliInterface`
- [ ] Refactor `CommandExecutor` to use CLI interfaces
- [ ] Update Docker execution strategies to be CLI-agnostic
- [ ] Update native execution strategies to be CLI-agnostic

### 2. Update command processing pipeline
- [ ] Refactor `CommandDataCollector` to work with generic structures
- [ ] Update `CommandExecutor` to use CLI interfaces
- [ ] Create CLI-specific command builders
- [ ] Update environment preparation to be CLI-agnostic
- [ ] Update cleanup processes to be CLI-agnostic

### 3. Migrate settings management
- [ ] Update settings panels to use new structure
- [ ] Create CLI selection UI components
- [ ] Migrate existing AiderSettings to new structure
- [ ] Update settings change listeners for new architecture
- [ ] Create settings validation for CLI-specific configurations

### 4. Create CLI-specific argument builders
- [ ] Implement Aider argument builder with current logic
- [ ] Implement Claude Code argument builder
- [ ] Implement Gemini CLI argument builder
- [ ] Create argument validation and error handling
- [ ] Build CLI-specific help and documentation

## Phase 3: UI and Actions Update (Week 5-6)

### 1. Make dialog components CLI-agnostic
- [ ] Update `AiderInputDialog` to use generic structures
- [ ] Create `CliOptionsPanel` for CLI-specific options
- [ ] Refactor `AiderOptionsPanel` to be generic
- [ ] Update completion providers to work with multiple CLIs
- [ ] Update model selection UI to be CLI-aware

### 2. Update actions to use CLI interfaces
- [ ] Refactor `AiderAction` and related actions to be CLI-agnostic
- [ ] Create CLI-specific action variants where needed
- [ ] Update action visibility and enablement logic
- [ ] Create CLI selection menu items
- [ ] Update context menu actions

### 3. Create CLI selection UI
- [ ] Add CLI selection dropdown to settings dialog
- [ ] Create CLI configuration panels
- [ ] Update tool windows to show current CLI information
- [ ] Create CLI-specific help and documentation
- [ ] Add CLI status indicators

### 4. Update settings panels
- [ ] Refactor `AiderSettingsConfigurable` to use new structure
- [ ] Create CLI-specific settings panels
- [ ] Update settings validation and persistence
- [ ] Create settings migration utilities
- [ ] Add CLI selection and configuration UI

## Phase 4: Feature Adaptation (Week 7-8)

### 1. Create CLI-specific feature implementations
- [ ] Implement Aider-specific features in `AiderCli`
- [ ] Create Claude Code-specific features
- [ ] Create Gemini CLI-specific features
- [ ] Build feature compatibility matrix
- [ ] Create feature toggles and settings

### 2. Handle feature compatibility
- [ ] Identify features that are Aider-only (plugin-based edits, sidecar mode)
- [ ] Create feature detection and graceful degradation
- [ ] Update UI to show/hide features based on selected CLI
- [ ] Create CLI-specific documentation

## Current State Analysis Findings

### Critical Issues Identified
- [ ] **87+ Aider-specific settings** in AiderSettings.kt need to be separated
- [ ] **CommandData.kt mixed concerns** - contains both generic and Aider-specific parameters
- [ ] **ModelConfiguration.kt** has hardcoded Aider-specific model mappings
- [ ] **Hardcoded argument patterns** throughout execution strategies
- [ ] **AiderMode enum** tightly coupled to Aider execution modes

### High Priority Refactoring Needed
- [ ] Separate generic settings from Aider-specific settings in AiderSettings.kt
- [ ] Extract Aider-specific parameters from CommandData.kt
- [ ] Create generic model configuration system
- [ ] Abstract argument building logic from execution strategies
- [ ] Replace AiderMode with generic CliMode

### Medium Priority Items
- [ ] Update API key management to be CLI-agnostic
- [ ] Refactor custom provider system
- [ ] Update Docker integration to support multiple CLIs
- [ ] Create CLI-specific documentation system

## Testing and Validation

### Unit Tests
- [ ] Create unit tests for CLI interface implementations
- [ ] Create unit tests for argument mapping
- [ ] Create unit tests for generic command data structures
- [ ] Create unit tests for settings migration

### Integration Tests
- [ ] Create integration tests for CLI execution
- [ ] Create integration tests for settings management
- [ ] Create integration tests for UI components
- [ ] Create integration tests for feature compatibility

### Manual Testing
- [ ] Test CLI switching functionality
- [ ] Test settings migration
- [ ] Test feature availability per CLI
- [ ] Test backward compatibility with existing configurations

## Documentation and Migration

### Documentation
- [ ] Create CLI abstraction architecture documentation
- [ ] Create CLI implementation guide
- [ ] Create feature compatibility matrix documentation
- [ ] Update user documentation for CLI selection

### Migration Support
- [ ] Create migration guide for existing users
- [ ] Create automated settings migration
- [ ] Create CLI selection wizard for new users
- [ ] Create troubleshooting guide for CLI issues

## Risk Mitigation

### Backward Compatibility
- [ ] Ensure existing Aider functionality is preserved
- [ ] Create fallback mechanisms for unsupported features
- [ ] Maintain existing API compatibility where possible
- [ ] Create migration path for existing configurations

### Performance Considerations
- [ ] Benchmark performance impact of abstraction layer
- [ ] Optimize CLI detection and initialization
- [ ] Minimize overhead in command execution
- [ ] Profile memory usage of new architecture

### Error Handling
- [ ] Create robust error handling for CLI detection failures
- [ ] Create graceful degradation for missing CLI tools
- [ ] Create clear error messages for configuration issues
- [ ] Create recovery mechanisms for failed CLI operations
