package de.andrena.codingaider.model

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.FileData
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class StashInfo(
    val name: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val fileCount: Int = 0
) {
    fun getDisplayName(): String {
        val formattedTime = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        return if (name.isBlank()) formattedTime else "$name ($formattedTime)"
    }
    
    fun getFileName(): String {
        val sanitizedName = if (name.isBlank()) 
            timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        else 
            name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        
        return ".aider.stash.$sanitizedName.yaml"
    }
}

object StashManager {
    fun getStashFiles(project: Project): List<File> {
        val projectPath = project.basePath ?: return emptyList()
        val dir = File(projectPath)
        return dir.listFiles { file -> 
            file.name.startsWith(".aider.stash.") && file.name.endsWith(".yaml") 
        }?.toList() ?: emptyList()
    }
    
    fun getStashInfo(stashFile: File): StashInfo {
        val namePattern = Regex("\\.aider\\.stash\\.(.*?)\\.yaml")
        val matchResult = namePattern.find(stashFile.name)
        val stashName = matchResult?.groupValues?.get(1) ?: ""
        
        // Try to parse timestamp from filename if it follows the date pattern
        val timestampPattern = Regex("(\\d{8}_\\d{6})")
        val timestampMatch = timestampPattern.find(stashName)
        val timestamp = if (timestampMatch != null) {
            try {
                LocalDateTime.parse(
                    timestampMatch.groupValues[1],
                    DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                )
            } catch (e: Exception) {
                LocalDateTime.ofEpochSecond(stashFile.lastModified() / 1000, 0, java.time.ZoneOffset.UTC)
            }
        } else {
            LocalDateTime.ofEpochSecond(stashFile.lastModified() / 1000, 0, java.time.ZoneOffset.UTC)
        }
        
        // Count files in stash
        val fileCount = try {
            val files = ContextFileHandler.readContextFile(stashFile, File(stashFile.parent).absolutePath)
            files.size
        } catch (e: Exception) {
            0
        }
        
        return StashInfo(
            name = if (stashName.matches(Regex("\\d{8}_\\d{6}"))) "" else stashName,
            timestamp = timestamp,
            fileCount = fileCount
        )
    }
}
