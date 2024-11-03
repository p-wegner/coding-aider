package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import de.andrena.codingaider.command.FileData
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.jar.JarFile

@Service
class FileExtractorService {
    companion object {
        @JvmStatic
        fun getInstance(): FileExtractorService = service()
    }

    fun extractFilesIfNeeded(files: List<FileData>): List<FileData> {
        val tempDir = Files.createTempDirectory("codingaider-files")
        return files.map { fileData ->
            if (fileData.filePath.endsWith(".jar")) {
                extractFromJar(fileData.filePath, tempDir)
            } else {
                fileData
            }
        }
    }

    private fun extractFromJar(jarPath: String, tempDir: Path): FileData {
        val jarFile = JarFile(jarPath)
        val entries = jarFile.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val entryPath = tempDir.resolve(entry.name)
            if (entry.isDirectory) {
                Files.createDirectories(entryPath)
            } else {
                jarFile.getInputStream(entry).use { input ->
                    Files.copy(input, entryPath, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
        jarFile.close()
        return FileData(tempDir.toString(), false)
    }
}

