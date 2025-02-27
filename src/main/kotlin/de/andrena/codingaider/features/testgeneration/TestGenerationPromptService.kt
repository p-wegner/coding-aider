package de.andrena.codingaider.features.testgeneration

import com.intellij.openapi.components.Service
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.services.AiderDocsService.Companion.AIDER_DOCS_FOLDER

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

        // Get configured reference files - ensure they're absolute paths
        val configuredReferenceFiles = testType.withAbsolutePaths(files.firstOrNull()?.filePath?.substringBefore("src") ?: "").contextFiles
            
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

    fun buildTestAbstractionPrompt(configuration: TestTypeConfiguration): String {
        return buildString {
            appendLine("Analyze the following files:")
            configuration.contextFiles.forEach { appendLine("- $it") }
            appendLine("The files should be used as a reference for patterns and conventions for test generation")
            appendLine("Based on the provided files, describe the patterns and conventions used for testing in a concise and clear manner")
            appendLine("The description should be structured and include all necessary information to write tests for similar files using the same patterns, testing libraries, and testing frameworks")
            appendLine("If unconventional patterns or conventions are used, describe them using simple and concise examples")
            appendLine()
            appendLine("Additional instructions that don't need to be explicitly included in the abstraction:")
            appendLine(configuration.promptTemplate)
            appendLine("Save your response as a markdown file named '${configuration.name}_abstraction.md' in the folder $AIDER_DOCS_FOLDER/testtypes ")
        }
    }
}
