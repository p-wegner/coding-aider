package de.andrena.codingaider.cli

/**
 * Data class representing a CLI-specific command ready for execution.
 * This contains the executable, arguments, and environment configuration
 * specific to a particular CLI tool.
 */
data class CliSpecificCommand(
    /**
     * The executable name or path
     */
    val executable: String,
    
    /**
     * List of command line arguments
     */
    val arguments: List<String>,
    
    /**
     * Environment variables for the command
     */
    val environment: Map<String, String> = emptyMap(),
    
    /**
     * Working directory for the command
     */
    val workingDirectory: String = "",
    
    /**
     * Timeout for the command in milliseconds
     */
    val timeout: Long = 0,
    
    /**
     * Whether to run the command in a Docker container
     */
    val useDocker: Boolean = false,
    
    /**
     * Docker image to use (if useDocker is true)
     */
    val dockerImage: String = "",
    
    /**
     * Docker volumes to mount (if useDocker is true)
     */
    val dockerVolumes: Map<String, String> = emptyMap(),
    
    /**
     * Docker environment variables (if useDocker is true)
     */
    val dockerEnvironment: Map<String, String> = emptyMap(),
    
    /**
     * Whether to run the command in sidecar mode
     */
    val useSidecar: Boolean = false,
    
    /**
     * Sidecar process ID (if useSidecar is true)
     */
    val sidecarProcessId: String? = null,
    
    /**
     * Additional command metadata
     */
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Gets the full command as a list of strings
     */
    fun getFullCommand(): List<String> {
        return listOf(executable) + arguments
    }
    
    /**
     * Gets the command as a single string (for logging/display)
     */
    fun getCommandString(): String {
        return getFullCommand().joinToString(" ")
    }
    
    /**
     * Creates a copy with additional arguments
     */
    fun withAdditionalArguments(additionalArgs: List<String>): CliSpecificCommand {
        return this.copy(arguments = this.arguments + additionalArgs)
    }
    
    /**
     * Creates a copy with additional environment variables
     */
    fun withAdditionalEnvironment(additionalEnv: Map<String, String>): CliSpecificCommand {
        return this.copy(environment = this.environment + additionalEnv)
    }
    
    /**
     * Creates a copy with additional metadata
     */
    fun withAdditionalMetadata(additionalMetadata: Map<String, String>): CliSpecificCommand {
        return this.copy(metadata = this.metadata + additionalMetadata)
    }
    
    /**
     * Validates the command
     * @return List of validation errors, empty if valid
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (executable.isBlank()) {
            errors.add("Executable cannot be blank")
        }
        
        if (useDocker && dockerImage.isBlank()) {
            errors.add("Docker image must be specified when useDocker is true")
        }
        
        if (useSidecar && sidecarProcessId.isNullOrBlank()) {
            errors.add("Sidecar process ID must be specified when useSidecar is true")
        }
        
        if (timeout < 0) {
            errors.add("Timeout cannot be negative")
        }
        
        return errors
    }
    
    /**
     * Checks if the command should use Docker
     */
    fun shouldUseDocker(): Boolean {
        return useDocker && dockerImage.isNotBlank()
    }
    
    /**
     * Checks if the command should use sidecar mode
     */
    fun shouldUseSidecar(): Boolean {
        return useSidecar && sidecarProcessId != null
    }
    
    /**
     * Gets the effective working directory
     */
    fun getEffectiveWorkingDirectory(): String {
        return if (workingDirectory.isBlank()) {
            System.getProperty("user.dir") ?: ""
        } else {
            workingDirectory
        }
    }
    
    companion object {
        /**
         * Creates a simple command with just executable and arguments
         */
        fun simple(executable: String, vararg arguments: String): CliSpecificCommand {
            return CliSpecificCommand(
                executable = executable,
                arguments = arguments.toList()
            )
        }
        
        /**
         * Creates a Docker command
         */
        fun docker(
            executable: String,
            dockerImage: String,
            vararg arguments: String,
            volumes: Map<String, String> = emptyMap()
        ): CliSpecificCommand {
            return CliSpecificCommand(
                executable = executable,
                arguments = arguments.toList(),
                useDocker = true,
                dockerImage = dockerImage,
                dockerVolumes = volumes
            )
        }
        
        /**
         * Creates a sidecar command
         */
        fun sidecar(
            executable: String,
            sidecarProcessId: String,
            vararg arguments: String
        ): CliSpecificCommand {
            return CliSpecificCommand(
                executable = executable,
                arguments = arguments.toList(),
                useSidecar = true,
                sidecarProcessId = sidecarProcessId
            )
        }
    }
}