package de.andrena.codingaider.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import de.andrena.codingaider.command.FileData
import java.io.File

data class ContextYamlFile(val path: String, val readOnly: Boolean = false)
data class ContextYamlData(val files: List<ContextYamlFile> = emptyList())

object ContextFileHandler {
    private val objectMapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())

    fun readContextFile(contextFile: File, projectBasePath: String): List<FileData> {
        return try {
            val yamlData: ContextYamlData = objectMapper.readValue(contextFile)
            yamlData.files.map {
                // TODO: handle all types of file paths (relative, absolute)
                FileData(File(projectBasePath, it.path).canonicalPath, it.readOnly)
            }
        } catch (e: Exception) {
            println("Error parsing context yaml ${contextFile.name}: ${e.message}")
            emptyList()
        }
    }

    fun writeContextFile(contextFile: File, files: List<FileData>, projectBasePath: String) {
        val yamlData = ContextYamlData(
            files = files.map { file ->
                ContextYamlFile(
                    path = File(file.filePath).relativeTo(File(projectBasePath)).path.replace('\\', '/'),
                    readOnly = file.isReadOnly
                )
            }
        )
        objectMapper.writeValue(contextFile, yamlData)
    }
}
