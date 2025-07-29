package de.andrena.codingaider.providers

/**
 * Enumeration of supported AI providers
 */
enum class AIProvider(val displayName: String, val executableName: String) {
    AIDER("Aider", "aider"),
    CLAUDE_CODE("Claude Code", "claude-code");
    
    companion object {
        fun fromString(name: String): AIProvider? {
            return values().find { it.name.equals(name, ignoreCase = true) }
        }
    }
}