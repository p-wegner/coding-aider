package de.andrena.codingaider.services

import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class AiderOutputSummaryService {
    fun createPrompt(message: String): String = """
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