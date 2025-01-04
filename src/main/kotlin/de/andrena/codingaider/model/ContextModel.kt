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
            yamlData.files.mapNotNull {
                try {
                    val file = if (File(it.path).isAbsolute) {
                        File(it.path)
                    } else {
                        File(projectBasePath, it.path)
                    }.canonicalFile
                    
                    if (!file.exists()) {
                        println("Warning: File ${file.path} from context ${contextFile.name} does not exist")
                        null
                    } else {
                        FileData(file.canonicalPath, it.readOnly)
                    }
                } catch (e: Exception) {
                    println("Error processing file path ${it.path} from context ${contextFile.name}: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            println("Error parsing context yaml ${contextFile.name}: ${e.message}")
            emptyList()
        }
    }

    fun writeContextFile(contextFile: File, files: List<FileData>, projectBasePath: String) {
        val yamlData = ContextYamlData(
            files = files.mapNotNull { file ->
                try {
                    val filePath = File(file.filePath)
                    val relativePath = if (filePath.isAbsolute) {
                        filePath.relativeTo(File(projectBasePath)).path
                    } else {
                        filePath.path
                    }.replace('\\', '/')
                    
                    ContextYamlFile(
                        path = relativePath,
                        readOnly = file.isReadOnly
                    )
                } catch (e: Exception) {
                    println("Error processing file path ${file.filePath} for context ${contextFile.name}: ${e.message}")
                    null
                }
            }
        )
        objectMapper.writeValue(contextFile, yamlData)
    }
}
