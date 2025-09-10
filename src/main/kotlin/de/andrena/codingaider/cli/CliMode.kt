package de.andrena.codingaider.cli

/**
 * Enum representing different execution modes for CLI tools.
 * This replaces the AiderMode enum to support multiple CLI tools.
 */
enum class CliMode(
    /**
     * The display name of the mode
     */
    val displayName: String,
    
    /**
     * The description of the mode
     */
    val description: String,
    
    /**
     * Whether this mode is supported by the CLI tool
     */
    val isSupported: Boolean = true,
    
    /**
     * The CLI-specific mode identifier
     */
    val cliIdentifier: String
) {
    /**
     * Normal mode - standard interaction with the AI
     */
    NORMAL(
        displayName = "Normal",
        description = "Standard interaction with the AI assistant",
        cliIdentifier = "normal"
    ),
    
    /**
     * Structured mode - for complex, multi-step tasks with planning
     */
    STRUCTURED(
        displayName = "Structured",
        description = "For complex, multi-step tasks with planning and checklists",
        cliIdentifier = "structured"
    ),
    
    /**
     * Architect mode - for architectural design and high-level planning
     */
    ARCHITECT(
        displayName = "Architect",
        description = "For architectural design and high-level planning",
        cliIdentifier = "architect"
    ),
    
    /**
     * Shell mode - for executing shell commands
     */
    SHELL(
        displayName = "Shell",
        description = "For executing shell commands and system operations",
        cliIdentifier = "shell",
        isSupported = false // Not all CLI tools support shell mode
    ),
    
    /**
     * Code review mode - for code review and analysis
     */
    CODE_REVIEW(
        displayName = "Code Review",
        description = "For code review and analysis tasks",
        cliIdentifier = "code-review",
        isSupported = false // Not all CLI tools support code review mode
    ),
    
    /**
     * Debug mode - for debugging and troubleshooting
     */
    DEBUG(
        displayName = "Debug",
        description = "For debugging and troubleshooting issues",
        cliIdentifier = "debug",
        isSupported = false // Not all CLI tools support debug mode
    ),
    
    /**
     * Refactor mode - for code refactoring
     */
    REFACTOR(
        displayName = "Refactor",
        description = "For code refactoring and optimization",
        cliIdentifier = "refactor",
        isSupported = false // Not all CLI tools support refactor mode
    ),
    
    /**
     * Test mode - for test generation and validation
     */
    TEST(
        displayName = "Test",
        description = "For test generation and validation",
        cliIdentifier = "test",
        isSupported = false // Not all CLI tools support test mode
    );
    
    /**
     * Checks if this mode is supported by the given CLI features
     * @param supportedFeatures Set of supported features by the CLI tool
     * @return true if the mode is supported, false otherwise
     */
    fun isSupportedBy(supportedFeatures: Set<CliFeature>): Boolean {
        if (!isSupported) return false
        
        return when (this) {
            NORMAL -> true // All CLI tools should support normal mode
            STRUCTURED -> CliFeature.STRUCTURED_MODE in supportedFeatures
            ARCHITECT -> CliFeature.STRUCTURED_MODE in supportedFeatures
            SHELL -> CliFeature.SHELL_MODE in supportedFeatures
            CODE_REVIEW -> CliFeature.CODE_REVIEW in supportedFeatures
            DEBUG -> CliFeature.DEBUGGING_ASSISTANCE in supportedFeatures
            REFACTOR -> CliFeature.REFACTORING_SUGGESTIONS in supportedFeatures
            TEST -> CliFeature.TEST_GENERATION in supportedFeatures
        }
    }
    
    /**
     * Gets the required features for this mode
     * @return Set of required features
     */
    fun getRequiredFeatures(): Set<CliFeature> {
        return when (this) {
            NORMAL -> emptySet()
            STRUCTURED -> setOf(CliFeature.STRUCTURED_MODE)
            ARCHITECT -> setOf(CliFeature.STRUCTURED_MODE)
            SHELL -> setOf(CliFeature.SHELL_MODE)
            CODE_REVIEW -> setOf(CliFeature.CODE_REVIEW)
            DEBUG -> setOf(CliFeature.DEBUGGING_ASSISTANCE)
            REFACTOR -> setOf(CliFeature.REFACTORING_SUGGESTIONS)
            TEST -> setOf(CliFeature.TEST_GENERATION)
        }
    }
    
    /**
     * Gets the recommended features for this mode
     * @return Set of recommended features
     */
    fun getRecommendedFeatures(): Set<CliFeature> {
        return when (this) {
            NORMAL -> setOf(CliFeature.FILE_OPERATIONS, CliFeature.CODE_GENERATION)
            STRUCTURED -> setOf(
                CliFeature.STRUCTURED_MODE,
                CliFeature.FILE_OPERATIONS,
                CliFeature.MULTI_FILE_PROCESSING,
                CliFeature.PLANNING_AND_TASK_MANAGEMENT
            )
            ARCHITECT -> setOf(
                CliFeature.STRUCTURED_MODE,
                CliFeature.ARCHITECTURE_DESIGN,
                CliFeature.MULTI_FILE_PROCESSING,
                CliFeature.CONTEXT_AWARENESS
            )
            SHELL -> setOf(
                CliFeature.SHELL_MODE,
                CliFeature.SHELL_COMMAND_EXECUTION,
                CliFeature.SYSTEM_PROMPT
            )
            CODE_REVIEW -> setOf(
                CliFeature.CODE_REVIEW,
                CliFeature.CODE_ANALYSIS,
                CliFeature.MULTI_FILE_PROCESSING
            )
            DEBUG -> setOf(
                CliFeature.DEBUGGING_ASSISTANCE,
                CliFeature.CODE_ANALYSIS,
                CliFeature.CONTEXT_AWARENESS
            )
            REFACTOR -> setOf(
                CliFeature.REFACTORING_SUGGESTIONS,
                CliFeature.CODE_ANALYSIS,
                CliFeature.FILE_EDITING
            )
            TEST -> setOf(
                CliFeature.TEST_GENERATION,
                CliFeature.CODE_ANALYSIS,
                CliFeature.FILE_CREATION
            )
        }
    }
    
    /**
     * Finds a mode by its CLI identifier
     * @param identifier The CLI identifier
     * @return The matching mode, or null if not found
     */
    companion object {
        fun fromCliIdentifier(identifier: String): CliMode? {
            return values().find { it.cliIdentifier == identifier }
        }
        
        /**
         * Gets all supported modes for the given CLI features
         * @param supportedFeatures Set of supported features by the CLI tool
         * @return List of supported modes
         */
        fun getSupportedModes(supportedFeatures: Set<CliFeature>): List<CliMode> {
            return values().filter { it.isSupportedBy(supportedFeatures) }
        }
        
        /**
         * Gets the default mode
         * @return The default mode (NORMAL)
         */
        fun getDefaultMode(): CliMode {
            return NORMAL
        }
    }
}