package de.andrena.codingaider.cli

/**
 * Enum representing features that can be supported by different CLI tools.
 * Used for feature detection and compatibility checking.
 */
enum class CliFeature {
    /**
     * Support for file input/output operations
     */
    FILE_OPERATIONS,
    
    /**
     * Support for read-only file access
     */
    READ_ONLY_FILES,
    
    /**
     * Support for automatic yes/no prompts
     */
    YES_FLAG,
    
    /**
     * Support for different edit formats
     */
    EDIT_FORMATS,
    
    /**
     * Support for linting integration
     */
    LINT_COMMAND,
    
    /**
     * Support for automatic commits
     */
    AUTO_COMMIT,
    
    /**
     * Support for dirty commits
     */
    DIRTY_COMMIT,
    
    /**
     * Support for shell mode
     */
    SHELL_MODE,
    
    /**
     * Support for structured/architect mode
     */
    STRUCTURED_MODE,
    
    /**
     * Support for reasoning effort configuration
     */
    REASONING_EFFORT,
    
    /**
     * Support for plugin-based edits
     */
    PLUGIN_BASED_EDITS,
    
    /**
     * Support for repository map deactivation
     */
    REPO_MAP_DEACTIVATION,
    
    /**
     * Support for change context inclusion
     */
    CHANGE_CONTEXT,
    
    /**
     * Support for sidecar mode
     */
    SIDECAR_MODE,
    
    /**
     * Support for Docker containerization
     */
    DOCKER_SUPPORT,
    
    /**
     * Support for custom API endpoints
     */
    CUSTOM_API_ENDPOINTS,
    
    /**
     * Support for model reasoning configuration
     */
    MODEL_REASONING,
    
    /**
     * Support for web crawling capabilities
     */
    WEB_CRAWL,
    
    /**
     * Support for documentation generation
     */
    DOCUMENTATION_GENERATION,
    
    /**
     * Support for project analysis
     */
    PROJECT_ANALYSIS,
    
    /**
     * Support for code review
     */
    CODE_REVIEW,
    
    /**
     * Support for refactoring suggestions
     */
    REFACTORING_SUGGESTIONS,
    
    /**
     * Support for test generation
     */
    TEST_GENERATION,
    
    /**
     * Support for debugging assistance
     */
    DEBUGGING_ASSISTANCE,
    
    /**
     * Support for performance optimization
     */
    PERFORMANCE_OPTIMIZATION,
    
    /**
     * Support for code generation
     */
    CODE_GENERATION,
    
    /**
     * Support for multi-file processing
     */
    MULTI_FILE_PROCESSING,
    
    /**
     * Support for planning and task management
     */
    PLANNING_AND_TASK_MANAGEMENT,
    
    /**
     * Support for architecture design
     */
    ARCHITECTURE_DESIGN,
    
    /**
     * Support for context awareness
     */
    CONTEXT_AWARENESS,
    
    /**
     * Support for shell command execution
     */
    SHELL_COMMAND_EXECUTION,
    
    /**
     * Support for system prompts
     */
    SYSTEM_PROMPT,
    
    /**
     * Support for code analysis
     */
    CODE_ANALYSIS,
    
    /**
     * Support for file editing
     */
    FILE_EDITING,
    
    /**
     * Support for file creation
     */
    FILE_CREATION
}