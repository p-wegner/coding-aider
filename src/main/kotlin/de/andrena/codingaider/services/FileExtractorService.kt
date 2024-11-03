package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import de.andrena.codingaider.command.FileData
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

    val tempDir = Files.createTempDirectory("codingaider-files")

    fun extractFilesIfNeeded(files: List<FileData>): List<FileData> {
        return files.mapNotNull(::extractFileIfNeeded)
    }

    fun extractFileIfNeeded(fileData: FileData): FileData? = if (fileData.filePath.contains(".jar!")) {
        val jarPath = fileData.filePath.substringBefore("!/")
        val jarFilePath = fileData.filePath.substringAfter("!/")
        extractFromJar(jarPath, jarFilePath, tempDir)
    } else {
        fileData
    }

    private fun extractFromJar(jarPath: String, jarFilePath: String, tempDir: Path): FileData? {
        val jarFile = JarFile(jarPath)
        // only extract the file with jarFilePath
        jarFile.getJarEntry(jarFilePath)?.let { entry ->

            val entryPath = tempDir.resolve(entry.name)
            jarFile.getInputStream(entry).use { input ->
                // create directories if needed
                Files.createDirectories(entryPath.parent)
                Files.copy(input, entryPath, StandardCopyOption.REPLACE_EXISTING)
            }
            jarFile.close()
            return FileData(entryPath.toString(), true)
        }
        return null
    }
}

