package de.andrena.codingaider.services

import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class AiderOutputSummaryService {
    fun createPrompt(message: String): String {
        // Check if message starts with a slash command
        if (message.startsWith("/")) {
            // Extract the command part (everything up to the first space)
            val commandParts = message.split(" ", limit = 2)
            val command = commandParts[0]
            val remainingMessage = if (commandParts.size > 1) commandParts[1] else ""
            
            // Return the command followed by the decorated prompt
            return """$command <SystemPrompt>
After doing the changes, summarize the changes using proper markdown output in a defined xml block. 
Use the following format:
<aider-intention>
Describe the changes you plan to make here
</aider-intention>
The actual changes go here
<aider-summary>
The summary content goes here
</aider-summary>
</SystemPrompt>
<UserPrompt>$remainingMessage</UserPrompt>
    """.trimIndent()
        }
        
        // Regular case without slash command
        return """
<SystemPrompt>
After doing the changes, summarize the changes using proper markdown output in a defined xml block. 
Use the following format:
<aider-intention>
Describe the changes you plan to make here
</aider-intention>
The actual changes go here
<aider-summary>
The summary content goes here
</aider-summary>
</SystemPrompt>
<UserPrompt>$message</UserPrompt>
    """.trimIndent()
    }
}
