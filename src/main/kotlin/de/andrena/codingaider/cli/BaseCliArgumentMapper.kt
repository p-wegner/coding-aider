package de.andrena.codingaider.cli

/**
 * Base implementation of CliArgumentMapper providing common functionality.
 * Specific CLI implementations can extend this class.
 */
abstract class BaseCliArgumentMapper : CliArgumentMapper {
    
    /**
     * Argument mappings for this CLI implementation.
     */
    protected abstract val argumentMappings: Map<GenericArgument, CliArgument>
    
    override fun mapGenericArgument(genericArg: GenericArgument, value: String): CliArgument {
        val cliArgument = argumentMappings[genericArg]
            ?: throw IllegalArgumentException("No mapping found for generic argument: $genericArg")
        
        // Create a new CLI argument with the provided value
        return cliArgument.copy(
            defaultValue = value,
            requiresValue = true
        )
    }
    
    override fun convertArguments(genericArgs: Map<GenericArgument, String>): List<String> {
        val arguments = mutableListOf<String>()
        
        // Process arguments in a specific order to maintain CLI compatibility
        val orderedArguments = orderArguments(genericArgs)
        
        orderedArguments.forEach { (genericArg, value) ->
            val cliArgument = argumentMappings[genericArg]
            if (cliArgument != null) {
                when {
                    cliArgument.isFlag -> arguments.add(cliArgument.flag)
                    cliArgument.requiresValue -> {
                        val formattedArg = cliArgument.format(value)
                        arguments.add(formattedArg)
                    }
                    else -> arguments.add(cliArgument.flag)
                }
            }
        }
        
        return arguments
    }
    
    /**
     * Orders arguments according to CLI-specific requirements.
     * Default implementation maintains insertion order, but can be overridden.
     * @param genericArgs Map of generic arguments to their values
     * @return Ordered map of arguments
     */
    protected open fun orderArguments(genericArgs: Map<GenericArgument, String>): Map<GenericArgument, String> {
        return genericArgs
    }
    
    /**
     * Validates the provided arguments.
     * @param genericArgs Map of generic arguments to their values
     * @return List of validation errors, empty if valid
     */
    open fun validateArguments(genericArgs: Map<GenericArgument, String>): List<String> {
        val errors = mutableListOf<String>()
        
        genericArgs.forEach { (genericArg, value) ->
            val cliArgument = argumentMappings[genericArg]
            if (cliArgument != null) {
                val validationErrors = cliArgument.validate(value)
                errors.addAll(validationErrors)
            } else {
                errors.add("No mapping found for generic argument: $genericArg")
            }
        }
        
        // Check for required arguments
        argumentMappings.values.forEach { cliArgument ->
            if (cliArgument.isRequired) {
                val hasMatchingGenericArg = genericArgs.keys.any { genericArg ->
                    argumentMappings[genericArg] == cliArgument
                }
                if (!hasMatchingGenericArg) {
                    errors.add("Required argument '${cliArgument.flag}' is missing")
                }
            }
        }
        
        // Check for mutually exclusive arguments
        val usedFlags = genericArgs.keys.mapNotNull { argumentMappings[it]?.flag }
        argumentMappings.values.forEach { cliArgument ->
            cliArgument.mutuallyExclusive.forEach { mutuallyExclusiveFlag ->
                if (cliArgument.flag in usedFlags && mutuallyExclusiveFlag in usedFlags) {
                    errors.add("Argument '${cliArgument.flag}' is mutually exclusive with '$mutuallyExclusiveFlag'")
                }
            }
        }
        
        return errors
    }
    
    /**
     * Gets all supported generic arguments for this CLI implementation.
     * @return Set of supported generic arguments
     */
    fun getSupportedGenericArguments(): Set<GenericArgument> {
        return argumentMappings.keys
    }
    
    /**
     * Checks if a generic argument is supported by this CLI implementation.
     * @param genericArg The generic argument to check
     * @return true if supported, false otherwise
     */
    fun isSupported(genericArg: GenericArgument): Boolean {
        return genericArg in argumentMappings
    }
    
    /**
     * Gets the CLI argument for a generic argument.
     * @param genericArg The generic argument
     * @return The CLI argument, or null if not supported
     */
    fun getCliArgument(genericArg: GenericArgument): CliArgument? {
        return argumentMappings[genericArg]
    }
    
    /**
     * Logs a message for debugging purposes.
     * @param message The message to log
     */
    protected fun logDebug(message: String) {
        // TODO: Implement proper logging
        println("[DEBUG] ${this::class.simpleName}: $message")
    }
    
    /**
     * Logs an error message.
     * @param message The error message to log
     */
    protected fun logError(message: String) {
        // TODO: Implement proper logging
        System.err.println("[ERROR] ${this::class.simpleName}: $message")
    }
}