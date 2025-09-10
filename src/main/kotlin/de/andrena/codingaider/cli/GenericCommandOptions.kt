package de.andrena.codingaider.cli

/**
 * Generic command options that can be used by different CLI tools.
 * This abstracts away the Aider-specific options from the original CommandOptions.
 */
data class GenericCommandOptions(
    /**
     * Whether to use Docker for execution
     */
    val useDocker: Boolean = false,
    
    /**
     * Whether to disable presentation of changes
     */
    val disablePresentation: Boolean = false,
    
    /**
     * Commit hash to compare with (for diff operations)
     */
    val commitHashToCompareWith: String? = null,
    
    /**
     * Whether to automatically commit changes
     */
    val autoCommit: Boolean? = null,
    
    /**
     * Whether to commit dirty changes before execution
     */
    val dirtyCommit: Boolean? = null,
    
    /**
     * Whether to augment the prompt with additional information
     */
    val promptAugmentation: Boolean? = null,
    
    /**
     * Whether to automatically confirm all prompts
     */
    val useYesFlag: Boolean = false,
    
    /**
     * The edit format to use
     */
    val editFormat: String? = null,
    
    /**
     * The lint command to run
     */
    val lintCommand: String? = null,
    
    /**
     * Whether to deactivate repository map
     */
    val deactivateRepoMap: Boolean = false,
    
    /**
     * Whether to use sidecar mode
     */
    val sidecarMode: Boolean = false,
    
    /**
     * Whether to include change context
     */
    val includeChangeContext: Boolean = false,
    
    /**
     * The reasoning effort level
     */
    val reasoningEffort: String? = null,
    
    /**
     * Additional CLI-specific options
     */
    val additionalOptions: Map<String, String> = emptyMap()
) {
    /**
     * Creates a copy with specific options overridden
     */
    fun withOverrides(
        useDocker: Boolean? = null,
        disablePresentation: Boolean? = null,
        autoCommit: Boolean? = null,
        dirtyCommit: Boolean? = null,
        useYesFlag: Boolean? = null,
        editFormat: String? = null,
        lintCommand: String? = null,
        sidecarMode: Boolean? = null
    ): GenericCommandOptions {
        return this.copy(
            useDocker = useDocker ?: this.useDocker,
            disablePresentation = disablePresentation ?: this.disablePresentation,
            autoCommit = autoCommit ?: this.autoCommit,
            dirtyCommit = dirtyCommit ?: this.dirtyCommit,
            useYesFlag = useYesFlag ?: this.useYesFlag,
            editFormat = editFormat ?: this.editFormat,
            lintCommand = lintCommand ?: this.lintCommand,
            sidecarMode = sidecarMode ?: this.sidecarMode
        )
    }
    
    /**
     * Validates the options
     * @return List of validation errors, empty if valid
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (editFormat != null && editFormat.isBlank()) {
            errors.add("Edit format cannot be blank if specified")
        }
        
        if (lintCommand != null && lintCommand.isBlank()) {
            errors.add("Lint command cannot be blank if specified")
        }
        
        if (reasoningEffort != null && reasoningEffort.isBlank()) {
            errors.add("Reasoning effort cannot be blank if specified")
        }
        
        return errors
    }
    
    companion object {
        /**
         * Default options
         */
        val DEFAULT = GenericCommandOptions()
        
        /**
         * Creates options optimized for quick operations
         */
        fun quickOptions(): GenericCommandOptions {
            return DEFAULT.copy(
                useYesFlag = true,
                disablePresentation = true,
                autoCommit = false
            )
        }
        
        /**
         * Creates options optimized for detailed operations
         */
        fun detailedOptions(): GenericCommandOptions {
            return DEFAULT.copy(
                useYesFlag = false,
                disablePresentation = false,
                autoCommit = true,
                includeChangeContext = true
            )
        }
        
        /**
         * Creates options optimized for batch operations
         */
        fun batchOptions(): GenericCommandOptions {
            return DEFAULT.copy(
                useYesFlag = true,
                disablePresentation = true,
                autoCommit = true,
                dirtyCommit = true,
                sidecarMode = true
            )
        }
    }
}