package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

/**
 * Service for extracting commit messages from LLM responses
 */
@Service(Service.Level.PROJECT)
class CommitMessageExtractorService(private val project: Project) {
    private val logger = Logger.getInstance(CommitMessageExtractorService::class.java)
    
    /**
     * Extracts a commit message from an LLM response
     * @param llmResponse The response from the LLM
     * @return The extracted commit message, or null if none was found
     */
    fun extractCommitMessage(llmResponse: String): String? {
        // Look for the <aider-commit-message> XML tag in the response
        val commitMessageRegex = """<aider-commit-message>\s*([\s\S]*?)\s*</aider-commit-message>""".toRegex()
        val match = commitMessageRegex.find(llmResponse)
        
        return match?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }?.also {
            logger.info("Extracted commit message: $it")
        }
    }
}
