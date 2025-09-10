# [Coding Aider Plan - Checklist]

# CLI Interface Abstraction Refactoring Checklist

## Phase 1: Core Abstraction (Week 1-2) ✅ COMPLETED

### 1. Create CLI interface hierarchy ✅
- [x] Define `CliInterface` base interface with core methods
- [x] Define `CliModelHandler` interface for model resolution
- [x] Define `CliArgumentMapper` interface for argument mapping
- [x] Create `GenericArgument` enum with common CLI arguments (50+ arguments)
- [x] Create `CliArgument` data class for CLI-specific arguments
- [x] Create `CliFeature` enum for feature detection (25+ features)
- [x] Create `ModelCapability` enum for model capabilities (30+ capabilities)
- [x] Implement base classes for CLI-specific functionality

### 2. Extract generic command data structures ✅
- [x] Create `GenericCommandData` class to replace Aider-specific CommandData
- [x] Create `GenericCommandOptions` class for common options
- [x] Create `CliMode` enum to replace AiderMode (8 modes)
- [x] Create `CliSpecificCommand` data class for runtime execution
- [x] Create `CommandDataConverter` for backward compatibility
- [x] Update FileData to be CLI-agnostic (already CLI-agnostic)

### 3. Refactor settings management ✅
- [x] Create `GenericCliSettings` service for common settings
- [x] Create `CommonExecutionOptions` data class
- [x] Extract CLI-specific settings from AiderSettings into `AiderSpecificSettings`
- [x] Create `ClaudeCodeSpecificSettings` service
- [x] Implement `SettingsFactory` pattern for CLI selection
- [x] Create settings migration utilities for existing configurations

### 4. Build argument mapping system ✅
- [x] Create `CliArgumentMapper` implementations for Aider and Claude Code
- [x] Build argument validation utilities
- [x] Build argument conversion utilities
- [x] Create CLI feature detection system
- [x] Create CLI feature compatibility matrix
- [x] Create `CliFactory` for managing CLI implementations

## Phase 2: Implementation Refactoring (Week 3-4) ✅ COMPLETED

### 1. Refactor execution strategies ✅
- [x] Update `AiderExecutionStrategy` to implement `CliInterface`
- [x] Create `ClaudeCodeExecutionStrategy` implementing `CliInterface`
- [x] Create `GeminiCliExecutionStrategy` implementing `CliInterface`
- [x] Refactor `CommandExecutor` to use CLI interfaces
- [x] Update Docker execution strategies to be CLI-agnostic
- [x] Update native execution strategies to be CLI-agnostic

### 2. Update command processing pipeline ✅
- [x] Refactor `CommandDataCollector` to work with generic structures
- [x] Update `CommandExecutor` to use CLI interfaces
- [x] Create CLI-specific command builders
- [x] Update environment preparation to be CLI-agnostic
- [x] Update cleanup processes to be CLI-agnostic

### 3. Migrate settings management ✅
- [x] Update settings panels to use new structure
- [x] Create CLI selection UI components
- [x] Migrate existing AiderSettings to new structure
- [x] Update settings change listeners for new architecture
- [x] Create settings validation for CLI-specific configurations

### 4. Create CLI-specific argument builders ✅
- [x] Implement Aider argument builder with current logic
- [x] Implement Claude Code argument builder
- [x] Implement Gemini CLI argument builder
- [x] Create argument validation and error handling
- [x] Build CLI-specific help and documentation

## Phase 3: UI and Actions Update (Week 5-6) ✅ COMPLETED

### 1. Make dialog components CLI-agnostic ✅
- [x] Update `AiderInputDialog` to use generic structures
- [x] Create `CliOptionsPanel` for CLI-specific options
- [x] Refactor `AiderOptionsPanel` to be generic
- [x] Update completion providers to work with multiple CLIs
- [x] Update model selection UI to be CLI-aware

### 2. Update actions to use CLI interfaces ✅
- [x] Refactor `AiderAction` and related actions to be CLI-agnostic
- [x] Create CLI-specific action variants where needed
- [x] Update action visibility and enablement logic
- [x] Create CLI selection menu items
- [x] Update context menu actions

### 3. Create CLI selection UI ✅
- [x] Add CLI selection dropdown to settings dialog
- [x] Create CLI configuration panels
- [x] Update tool windows to show current CLI information
- [x] Create CLI-specific help and documentation
- [x] Add CLI status indicators

### 4. Update settings panels ✅
- [x] Refactor `AiderSettingsConfigurable` to use new structure
- [x] Create CLI-specific settings panels
- [x] Update settings validation and persistence
- [x] Create settings migration utilities
- [x] Add CLI selection and configuration UI

## Phase 4: Feature Adaptation (Week 7-8) ✅ COMPLETED

### 1. Create CLI-specific feature implementations ✅
- [x] Implement Aider-specific features in `AiderCli`
- [x] Create Claude Code-specific features
- [x] Create Gemini CLI-specific features
- [x] Build feature compatibility matrix
- [x] Create feature toggles and settings

### 2. Handle feature compatibility ✅
- [x] Identify features that are Aider-only (plugin-based edits, sidecar mode)
- [x] Create feature detection and graceful degradation
- [x] Update UI to show/hide features based on selected CLI
- [x] Create CLI-specific documentation

## Current State Analysis Findings - COMPLETED ✅

### Critical Issues Identified ✅ RESOLVED
- [x] **87+ Aider-specific settings** in AiderSettings.kt separated into GenericCliSettings and AiderSpecificSettings
- [x] **CommandData.kt mixed concerns** - resolved with GenericCommandData and CommandDataConverter
- [x] **ModelConfiguration.kt** - abstracted into CliModelHandler implementations
- [x] **Hardcoded argument patterns** - abstracted into CliArgumentMapper implementations
- [x] **AiderMode enum** - replaced with generic CliMode enum

### High Priority Refactoring Needed ✅ COMPLETED
- [x] Separate generic settings from Aider-specific settings in AiderSettings.kt
- [x] Extract Aider-specific parameters from CommandData.kt
- [x] Create generic model configuration system (CliModelHandler)
- [x] Abstract argument building logic from execution strategies (CliArgumentMapper)
- [x] Replace AiderMode with generic CliMode

### Medium Priority Items ✅ COMPLETED
- [x] Update API key management to be CLI-agnostic
- [x] Refactor custom provider system
- [x] Update Docker integration to support multiple CLIs
- [x] Create CLI-specific documentation system

---

## 🎉 **IMPLEMENTATION COMPLETE** 

The CLI Interface Abstraction Refactoring has been successfully implemented! All major phases are completed:

### Key Achievements:
- ✅ **Core Abstraction Layer**: Complete CLI interface hierarchy with 25+ features and 50+ arguments
- ✅ **Generic Command System**: Unified command data structures and processing pipeline
- ✅ **Settings Management**: Centralized settings with CLI-specific configurations
- ✅ **Execution Strategies**: CLI-agnostic execution with factory pattern
- ✅ **UI Components**: Dynamic UI that adapts to selected CLI tool
- ✅ **Backward Compatibility**: All existing functionality preserved
- ✅ **Extensibility**: New CLI tools can be added easily

### Architecture Benefits:
- **Maintainability**: Clear separation of concerns between generic and CLI-specific code
- **Extensibility**: New CLI tools can be added by implementing CliInterface
- **Feature Detection**: Dynamic capability checking for each CLI tool
- **Unified Settings**: Single settings interface for all CLI tools
- **Adaptive UI**: Interface that changes based on selected CLI capabilities

The system now supports multiple AI coding assistants while maintaining full backward compatibility with existing Aider functionality.