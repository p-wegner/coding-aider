package de.andrena.codingaider.cli

/**
 * Enum representing generic command line arguments that can be mapped to
 * CLI-specific arguments for different AI coding assistants.
 */
enum class GenericArgument {
    /**
     * The main prompt or question for the AI
     */
    PROMPT,
    
    /**
     * The model to use for generation
     */
    MODEL,
    
    /**
     * File to be included in context (writable)
     */
    FILE,
    
    /**
     * File to be included in context (read-only)
     */
    READ_ONLY_FILE,
    
    /**
     * Automatic yes/no flag for prompts
     */
    YES_FLAG,
    
    /**
     * Edit format specification
     */
    EDIT_FORMAT,
    
    /**
     * Lint command to run
     */
    LINT_COMMAND,
    
    /**
     * Enable/disable auto commits
     */
    AUTO_COMMIT,
    
    /**
     * Enable/disable dirty commits
     */
    DIRTY_COMMIT,
    
    /**
     * Reasoning effort level
     */
    REASONING_EFFORT,
    
    /**
     * Deactivate repository map
     */
    DEACTIVATE_REPO_MAP,
    
    /**
     * Include change context
     */
    INCLUDE_CHANGE_CONTEXT,
    
    /**
     * Enable shell mode
     */
    SHELL_MODE,
    
    /**
     * Enable structured/architect mode
     */
    STRUCTURED_MODE,
    
    /**
     * Enable sidecar mode
     */
    SIDECAR_MODE,
    
    /**
     * Additional custom arguments
     */
    ADDITIONAL_ARGUMENT,
    
    /**
     * Verbose logging flag
     */
    VERBOSE,
    
    /**
     * Quiet mode flag
     */
    QUIET,
    
    /**
     * Output format specification
     */
    OUTPUT_FORMAT,
    
    /**
     * Temperature parameter for generation
     */
    TEMPERATURE,
    
    /**
     * Max tokens parameter for generation
     */
    MAX_TOKENS,
    
    /**
     * Top-p parameter for generation
     */
    TOP_P,
    
    /**
     * Top-k parameter for generation
     */
    TOP_K,
    
    /**
     * Frequency penalty parameter
     */
    FREQUENCY_PENALTY,
    
    /**
     * Presence penalty parameter
     */
    PRESENCE_PENALTY,
    
    /**
     * Stop sequences parameter
     */
    STOP_SEQUENCES,
    
    /**
     * System prompt
     */
    SYSTEM_PROMPT,
    
    /**
     * User context
     */
    USER_CONTEXT,
    
    /**
     * Project directory
     */
    PROJECT_DIRECTORY,
    
    /**
     * Working directory
     */
    WORKING_DIRECTORY,
    
    /**
     * Configuration file path
     */
    CONFIG_FILE,
    
    /**
     * Cache directory
     */
    CACHE_DIRECTORY,
    
    /**
     * Log file path
     */
    LOG_FILE,
    
    /**
     * Timeout duration
     */
    TIMEOUT,
    
    /**
     * Retry count
     */
    RETRY_COUNT,
    
    /**
     * API key
     */
    API_KEY,
    
    /**
     * API base URL
     */
    API_BASE_URL,
    
    /**
     * API version
     */
    API_VERSION,
    
    /**
     * Custom headers
     */
    CUSTOM_HEADERS,
    
    /**
     * Proxy configuration
     */
    PROXY,
    
    /**
     * SSL verification
     */
    SSL_VERIFY,
    
    /**
     * Debug mode flag
     */
    DEBUG,
    
    /**
     * Dry run flag
     */
    DRY_RUN,
    
    /**
     * Force flag
     */
    FORCE,
    
    /**
     * Interactive mode flag
     */
    INTERACTIVE,
    
    /**
     * Batch mode flag
     */
    BATCH_MODE,
    
    /**
     * Continue on error flag
     */
    CONTINUE_ON_ERROR,
    
    /**
     * Show help flag
     */
    HELP,
    
    /**
     * Show version flag
     */
    VERSION,
    
    /**
     * Custom flag
     */
    CUSTOM_FLAG,
    
    /**
     * Enable streaming responses
     */
    STREAMING_RESPONSES,
    
    /**
     * Docker configuration
     */
    DOCKER_IMAGE,
    
    /**
     * Plugin-based edits flag
     */
    PLUGIN_BASED_EDITS
}