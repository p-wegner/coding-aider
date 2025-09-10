package de.andrena.codingaider.cli

/**
 * Data class representing a CLI-specific argument.
 * Contains the flag name, value format, and additional metadata.
 */
data class CliArgument(
    /**
     * The flag name (e.g., "--file", "-m")
     */
    val flag: String,
    
    /**
     * The value format (e.g., "=<value>", " <value>", "")
     */
    val valueFormat: String = "=<value>",
    
    /**
     * Whether this argument requires a value
     */
    val requiresValue: Boolean = true,
    
    /**
     * Whether this argument is a flag (no value needed)
     */
    val isFlag: Boolean = false,
    
    /**
     * Description of the argument
     */
    val description: String = "",
    
    /**
     * Whether this argument is required
     */
    val isRequired: Boolean = false,
    
    /**
     * Default value for the argument
     */
    val defaultValue: String? = null,
    
    /**
     * Possible values for the argument (for enum-like arguments)
     */
    val possibleValues: List<String> = emptyList(),
    
    /**
     * Whether this argument can be specified multiple times
     */
    val allowMultiple: Boolean = false,
    
    /**
     * The position of this argument in the command line (for ordered arguments)
     */
    val position: Int = -1,
    
    /**
     * Dependencies on other arguments
     */
    val dependencies: List<String> = emptyList(),
    
    /**
     * Mutually exclusive arguments
     */
    val mutuallyExclusive: List<String> = emptyList()
) {
    /**
     * Formats the argument with the given value.
     * @param value The value to format with the argument
     * @return The formatted argument string
     */
    fun format(value: String? = null): String {
        return when {
            isFlag -> flag
            requiresValue && value != null -> flag + valueFormat.replace("<value>", value)
            requiresValue && defaultValue != null -> flag + valueFormat.replace("<value>", defaultValue)
            else -> flag
        }
    }
    
    /**
     * Validates the given value for this argument.
     * @param value The value to validate
     * @return List of validation errors, empty if valid
     */
    fun validate(value: String?): List<String> {
        val errors = mutableListOf<String>()
        
        if (isRequired && value.isNullOrBlank() && defaultValue == null) {
            errors.add("Argument '$flag' is required")
        }
        
        if (!possibleValues.isEmpty() && value != null && value !in possibleValues) {
            errors.add("Argument '$flag' must be one of: ${possibleValues.joinToString(", ")}")
        }
        
        return errors
    }
    
    companion object {
        /**
         * Creates a flag argument (no value required).
         * @param flag The flag name
         * @param description Description of the flag
         * @return A CliArgument representing a flag
         */
        fun flag(flag: String, description: String = ""): CliArgument {
            return CliArgument(
                flag = flag,
                valueFormat = "",
                requiresValue = false,
                isFlag = true,
                description = description
            )
        }
        
        /**
         * Creates a value argument with default format.
         * @param flag The flag name
         * @param description Description of the argument
         * @return A CliArgument representing a value argument
         */
        fun value(flag: String, description: String = ""): CliArgument {
            return CliArgument(
                flag = flag,
                valueFormat = "=<value>",
                requiresValue = true,
                isFlag = false,
                description = description
            )
        }
        
        /**
         * Creates a positional argument.
         * @param position The position index
         * @param description Description of the argument
         * @return A CliArgument representing a positional argument
         */
        fun positional(position: Int, description: String = ""): CliArgument {
            return CliArgument(
                flag = "",
                valueFormat = "<value>",
                requiresValue = true,
                isFlag = false,
                description = description,
                position = position
            )
        }
    }
}