package de.andrena.codingaider.services

import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.settings.AiderProjectSettings.TestTypeConfiguration

class TestGenerationPromptService {
    fun buildPrompt(
        testType: TestTypeConfiguration,
        files: List<FileData>,
        additionalPrompt: String
    ): String {
        val (sourceFiles, referenceFiles) = files.partition { file ->
            !file.filePath.matches(Regex(testType.referenceFilePattern))
        }
        
        val sourceFileNames = sourceFiles.map { it.filePath }
        val referenceFileNames = referenceFiles.map { it.filePath }
        
        return buildString {
            appendLine("Generate tests for the following files: $sourceFileNames")
            appendLine("Test type: ${testType.name}")
            appendLine()
            appendLine("Reference files to use as examples: $referenceFileNames")
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
