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
                val absolutePath = if (File(it.path).isAbsolute) {
                    it.path
                } else {
                    File(projectBasePath, it.path).canonicalPath
                }
                FileData(absolutePath, it.readOnly)
            }
        } catch (e: Exception) {
            println("Error parsing context yaml ${contextFile.name}: ${e.message}")
            emptyList()
        }
    }

    fun writeContextFile(contextFile: File, files: List<FileData>) {
        val yamlData = ContextYamlData(
            files = files.map { file ->
                ContextYamlFile(
                    path = file.filePath,
                    readOnly = file.isReadOnly
                )
            }
        )
        objectMapper.writeValue(contextFile, yamlData)
    }
}
