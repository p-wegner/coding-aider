package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.settings.AiderProjectSettings.TestTypeConfiguration

@Service(Service.Level.PROJECT)
class TestGenerationPromptService {
    fun buildPrompt(
        testType: TestTypeConfiguration,
        files: List<FileData>,
        additionalPrompt: String
    ): String {
        // Files selected for test generation
        val selectedFiles = files.map { it.filePath }
        
        // Find existing test files that match the pattern
        val existingTestFiles = files
            .filter { it.filePath.matches(Regex(testType.referenceFilePattern)) }
            .map { it.filePath }

        // Get configured reference files
        val configuredReferenceFiles = testType.contextFiles
            
        return buildString {
            appendLine("Generate tests for the following files:")
            selectedFiles.forEach { appendLine("- $it") }
            appendLine()
            
            if (existingTestFiles.isNotEmpty()) {
                appendLine("Use these existing test files as reference for patterns and conventions:")
                existingTestFiles.forEach { appendLine("- $it") }
                appendLine()
            }
            
            if (configuredReferenceFiles.isNotEmpty()) {
                appendLine("Additional reference materials:")
                configuredReferenceFiles.forEach { appendLine("- $it") }
                appendLine()
            }
            
            appendLine("Test files will be generated using pattern: ${testType.testFilePattern}")
            appendLine()
            appendLine("Instructions:")
            appendLine(testType.promptTemplate)
            
            if (additionalPrompt.trim().isNotEmpty()) {
                appendLine()
                appendLine("Additional instructions:")
                appendLine(additionalPrompt.trim())
            }
        }.trimEnd()
    }
}
