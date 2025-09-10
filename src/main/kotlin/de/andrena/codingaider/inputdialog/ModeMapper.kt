package de.andrena.codingaider.inputdialog

import de.andrena.codingaider.cli.CliMode

/**
 * Utility class for mapping between AiderMode and CliMode.
 * This provides backward compatibility during the transition.
 */
object ModeMapper {
    
    /**
     * Maps from AiderMode to CliMode.
     * @param aiderMode The AiderMode to map
     * @return The corresponding CliMode
     */
    fun fromAiderMode(aiderMode: AiderMode): CliMode {
        return when (aiderMode) {
            AiderMode.NORMAL -> CliMode.NORMAL
            AiderMode.ARCHITECT -> CliMode.ARCHITECT
            AiderMode.STRUCTURED -> CliMode.STRUCTURED
            AiderMode.SHELL -> CliMode.SHELL
        }
    }
    
    /**
     * Maps from CliMode to AiderMode.
     * @param cliMode The CliMode to map
     * @return The corresponding AiderMode, or null if no mapping exists
     */
    fun toAiderMode(cliMode: CliMode): AiderMode? {
        return when (cliMode) {
            CliMode.NORMAL -> AiderMode.NORMAL
            CliMode.ARCHITECT -> AiderMode.ARCHITECT
            CliMode.STRUCTURED -> AiderMode.STRUCTURED
            CliMode.SHELL -> AiderMode.SHELL
            else -> null
        }
    }
    
    /**
     * Gets the supported AiderMode entries based on supported CliModes.
     * @param supportedCliModes List of supported CliModes
     * @return List of supported AiderMode entries
     */
    fun getSupportedAiderModes(supportedCliModes: List<CliMode>): List<AiderMode> {
        return AiderMode.entries.filter { aiderMode ->
            val cliMode = fromAiderMode(aiderMode)
            supportedCliModes.contains(cliMode)
        }
    }
    
    /**
     * Checks if an AiderMode is supported based on the CLI tool's capabilities.
     * @param aiderMode The AiderMode to check
     * @param supportedCliModes List of supported CliModes
     * @return true if the mode is supported, false otherwise
     */
    fun isModeSupported(aiderMode: AiderMode, supportedCliModes: List<CliMode>): Boolean {
        val cliMode = fromAiderMode(aiderMode)
        return supportedCliModes.contains(cliMode)
    }
}