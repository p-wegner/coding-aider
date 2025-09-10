package de.andrena.codingaider.cli

/**
 * Interface for mapping generic arguments to CLI-specific arguments.
 * Different AI coding assistants use different command line argument formats.
 */
interface CliArgumentMapper {
    /**
     * Maps a generic argument to a CLI-specific argument.
     * @param genericArg The generic argument type
     * @param value The value for the argument
     * @return The CLI-specific argument representation
     */
    fun mapGenericArgument(genericArg: GenericArgument, value: String): CliArgument
    
    /**
     * Gets the prompt argument for this CLI tool.
     * @return The CLI-specific prompt argument
     */
    fun getPromptArgument(): CliArgument
    
    /**
     * Gets the file argument for this CLI tool.
     * @param isReadOnly Whether the file is read-only
     * @return The CLI-specific file argument
     */
    fun getFileArgument(isReadOnly: Boolean): CliArgument
    
    /**
     * Gets the model argument for this CLI tool.
     * @return The CLI-specific model argument
     */
    fun getModelArgument(): CliArgument
    
    /**
     * Gets the yes flag argument for this CLI tool.
     * @return The CLI-specific yes flag argument, or null if not supported
     */
    fun getYesFlagArgument(): CliArgument?
    
    /**
     * Gets the edit format argument for this CLI tool.
     * @return The CLI-specific edit format argument, or null if not supported
     */
    fun getEditFormatArgument(): CliArgument?
    
    /**
     * Gets the lint command argument for this CLI tool.
     * @return The CLI-specific lint command argument, or null if not supported
     */
    fun getLintCommandArgument(): CliArgument?
    
    /**
     * Gets the auto-commit argument for this CLI tool.
     * @param enabled Whether auto-commit should be enabled
     * @return The CLI-specific auto-commit argument, or null if not supported
     */
    fun getAutoCommitArgument(enabled: Boolean): CliArgument?
    
    /**
     * Gets the dirty-commit argument for this CLI tool.
     * @param enabled Whether dirty-commit should be enabled
     * @return The CLI-specific dirty-commit argument, or null if not supported
     */
    fun getDirtyCommitArgument(enabled: Boolean): CliArgument?
    
    /**
     * Converts a list of generic arguments to CLI-specific arguments.
     * @param genericArgs Map of generic arguments to their values
     * @return List of CLI-specific arguments ready for command line execution
     */
    fun convertArguments(genericArgs: Map<GenericArgument, String>): List<String>
}