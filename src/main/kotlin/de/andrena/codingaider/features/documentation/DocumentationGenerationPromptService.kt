package de.andrena.codingaider.features.documentation

import com.intellij.openapi.components.Service
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.services.AiderDocsService.Companion.AIDER_DOCS_FOLDER

@Service(Service.Level.PROJECT)
class DocumentationGenerationPromptService {
    fun buildPrompt(
        documentType: DocumentTypeConfiguration,
        files: List<FileData>,
        filename: String,
        additionalPrompt: String = ""
    ): String {
        // Files selected for documentation generation
        val selectedFiles = files.map { it.filePath }
        
        // Get configured reference files - ensure they're absolute paths
        val configuredReferenceFiles = documentType.withAbsolutePaths(files.firstOrNull()?.filePath?.substringBefore("src") ?: "").contextFiles
            
        return buildString {
            appendLine("Generate documentation for the following files:")
            selectedFiles.forEach { appendLine("- $it") }
            appendLine()
            
            if (configuredReferenceFiles.isNotEmpty()) {
                appendLine("Additional reference materials:")
                configuredReferenceFiles.forEach { appendLine("- $it") }
                appendLine()
            }
            
            appendLine("Store the results in $filename")
            appendLine()
            appendLine("Instructions:")
            appendLine(documentType.promptTemplate)
            
            if (additionalPrompt.trim().isNotEmpty()) {
                appendLine()
                appendLine("Additional instructions:")
                appendLine(additionalPrompt.trim())
            }
            
            appendLine()
            appendLine("If the file already exists, update it instead of creating a new one.")
        }.trimEnd()
    }

    fun buildDocumentationAbstractionPrompt(configuration: DocumentTypeConfiguration): String {
        return buildString {
            appendLine("Analyze the following files:")
            configuration.contextFiles.forEach { appendLine("- $it") }
            appendLine("The files should be used as a reference for patterns and conventions for documentation generation")
            appendLine("Based on the provided files, describe the patterns and conventions used for documentation in a concise and clear manner")
            appendLine("The description should be structured and include all necessary information to write documentation for similar files using the same patterns and conventions")
            appendLine("If unconventional patterns or conventions are used, describe them using simple and concise examples")
            appendLine()
            appendLine("Additional instructions that don't need to be explicitly included in the abstraction:")
            appendLine(configuration.promptTemplate)
            appendLine("Save your response as a markdown file named '${configuration.name}_abstraction.md' in the folder $AIDER_DOCS_FOLDER/doctypes")
        }
    }
}
