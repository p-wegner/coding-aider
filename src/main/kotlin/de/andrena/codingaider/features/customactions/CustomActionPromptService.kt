package de.andrena.codingaider.features.customactions

import com.intellij.openapi.components.Service
import de.andrena.codingaider.command.FileData

@Service(Service.Level.PROJECT)
class CustomActionPromptService {
    fun buildPrompt(
        customAction: CustomActionConfiguration,
        files: List<FileData>,
        additionalPrompt: String
    ): String {
        // Files selected for the custom action
        val selectedFiles = files.map { it.filePath }
        
        // Get configured context files - ensure they're absolute paths
        val configuredContextFiles = customAction.withAbsolutePaths(files.firstOrNull()?.filePath?.substringBefore("src") ?: "").contextFiles
            
        return buildString {
            appendLine("Execute custom action '${customAction.name}' on the following files:")
            selectedFiles.forEach { appendLine("- $it") }
            appendLine()
            
            if (configuredContextFiles.isNotEmpty()) {
                appendLine("Additional context files:")
                configuredContextFiles.forEach { appendLine("- $it") }
                appendLine()
            }
            
            appendLine("Instructions:")
            appendLine(customAction.promptTemplate)
            
            if (additionalPrompt.trim().isNotEmpty()) {
                appendLine()
                appendLine("Additional instructions:")
                appendLine(additionalPrompt.trim())
            }
        }.trimEnd()
    }
}
