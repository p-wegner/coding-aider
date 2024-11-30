package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding
import com.knuddels.jtokkit.api.EncodingRegistry
import com.knuddels.jtokkit.api.ModelType
import de.andrena.codingaider.command.FileData
import java.io.File

@Service(Service.Level.PROJECT)
class TokenCountService(private val project: Project) {

    private val registry: EncodingRegistry = Encodings.newDefaultEncodingRegistry()
    private val encoding: Encoding = registry.getEncodingForModel(ModelType.GPT_4O)

    fun countTokensInText(text: String, encoding: Encoding = this.encoding): Int {
        return encoding.countTokens(text)
    }

    fun countTokensInFiles(files: List<FileData>,encoding: Encoding = this.encoding): Int {
        return files.sumOf { fileData ->
            kotlin.runCatching {
                val fileContent = File(fileData.filePath).readText()
                countTokensInText(fileContent, encoding)
            }.getOrDefault(0)
        }
    }

    companion object {
        private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "bmp", "svg", "ico")
        private const val MAX_FILE_SIZE_BYTES = 500 * 1024 // 500KB
    }

    fun shouldSkipTokenCount(filePath: String): Boolean {
        val file = File(filePath)
        return isImage(filePath) || isTooBig(file)
    }

    private fun isImage(filePath: String): Boolean {
        val extension = filePath.substringAfterLast('.', "").lowercase()
        return extension in IMAGE_EXTENSIONS
    }

    private fun isTooBig(file: File): Boolean {
        return file.exists() && file.length() > MAX_FILE_SIZE_BYTES
    }
}
