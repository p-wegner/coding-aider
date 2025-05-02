package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.settings.AiderSettings

@Service(Service.Level.PROJECT)
class AiderPromptAugmentationService(private val project: Project) {
    private val settings = AiderSettings.getInstance()
    
    fun createPrompt(message: String): String {
        if (!settings.promptAugmentation) {
            return message
        }
        
        // Check if message starts with a slash command
        if (message.startsWith("/")) {
            // Extract the command part (everything up to the first space)
            val commandParts = message.split(" ", limit = 2)
            val command = commandParts[0]
            val remainingMessage = if (commandParts.size > 1) commandParts[1] else ""
            
            // Return the command followed by the decorated prompt
            return """$command ${createSystemPrompt()}
<UserPrompt>$remainingMessage</UserPrompt>
    """.trimIndent()
        }
        
        // Regular case without slash command
        return """
${createSystemPrompt()}
<UserPrompt>$message</UserPrompt>
    """.trimIndent()
    }
    
    private fun createSystemPrompt(): String {
        val basePrompt = """
<SystemPrompt>
After doing the changes, summarize the changes using proper markdown output in a defined xml block. 
Use the following format:
<aider-intention>
Describe the changes you plan to make here
</aider-intention>
The actual changes go here
<aider-summary>
The summary content goes here
</aider-summary>"""
        
        val commitMessageBlock = if (settings.includeCommitMessageBlock) {
            """
<aider-commit-message>
Write a concise commit message using conventional commits in the format: <type>: <description>
Use these for <type>: fix, feat, build, chore, ci, docs, style, refactor, perf, test
</aider-commit-message>"""
        } else {
            ""
        }
        
        return "$basePrompt$commitMessageBlock\n</SystemPrompt>"
    }
}
