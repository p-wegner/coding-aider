package de.andrena.codingaider.cli

/**
 * Enum representing model capabilities that can be checked by different CLI tools.
 */
enum class ModelCapability {
    /**
     * Basic text generation capability
     */
    TEXT_GENERATION,
    
    /**
     * Code generation capability
     */
    CODE_GENERATION,
    
    /**
     * Code analysis capability
     */
    CODE_ANALYSIS,
    
    /**
     * Natural language understanding
     */
    NATURAL_LANGUAGE_UNDERSTANDING,
    
    /**
     * Natural language generation
     */
    NATURAL_LANGUAGE_GENERATION,
    
    /**
     * Multi-file processing capability
     */
    MULTI_FILE_PROCESSING,
    
    /**
     * File editing capability
     */
    FILE_EDITING,
    
    /**
     * File creation capability
     */
    FILE_CREATION,
    
    /**
     * File deletion capability
     */
    FILE_DELETION,
    
    /**
     * Directory traversal capability
     */
    DIRECTORY_TRAVERSAL,
    
    /**
     * Git integration capability
     */
    GIT_INTEGRATION,
    
    /**
     * Shell command execution
     */
    SHELL_COMMAND_EXECUTION,
    
    /**
     * Web search capability
     */
    WEB_SEARCH,
    
    /**
     * Web crawling capability
     */
    WEB_CRAWL,
    
    /**
     * Documentation generation
     */
    DOCUMENTATION_GENERATION,
    
    /**
     * Code review capability
     */
    CODE_REVIEW,
    
    /**
     * Refactoring assistance
     */
    REFACTORING_ASSISTANCE,
    
    /**
     * Debugging assistance
     */
    DEBUGGING_ASSISTANCE,
    
    /**
     * Test generation
     */
    TEST_GENERATION,
    
    /**
     * Performance optimization
     */
    PERFORMANCE_OPTIMIZATION,
    
    /**
     * Security analysis
     */
    SECURITY_ANALYSIS,
    
    /**
     * Architecture design
     */
    ARCHITECTURE_DESIGN,
    
    /**
     * Planning and task management
     */
    PLANNING_AND_TASK_MANAGEMENT,
    
    /**
     * Reasoning and problem solving
     */
    REASONING_AND_PROBLEM_SOLVING,
    
    /**
     * Multi-turn conversation
     */
    MULTI_TURN_CONVERSATION,
    
    /**
     * Context awareness
     */
    CONTEXT_AWARENESS,
    
    /**
     * Long context processing
     */
    LONG_CONTEXT_PROCESSING,
    
    /**
     * Streaming responses
     */
    STREAMING_RESPONSES,
    
    /**
     * Batch processing
     */
    BATCH_PROCESSING,
    
    /**
     * Real-time processing
     */
    REAL_TIME_PROCESSING,
    
    /**
     * Parallel processing
     */
    PARALLEL_PROCESSING,
    
    /**
     * Custom model capabilities
     */
    CUSTOM_CAPABILITY
}