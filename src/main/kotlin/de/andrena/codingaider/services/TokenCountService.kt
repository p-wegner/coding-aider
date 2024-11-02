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
}
