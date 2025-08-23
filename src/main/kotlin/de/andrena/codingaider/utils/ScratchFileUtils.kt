package de.andrena.codingaider.utils

import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.vfs.VirtualFile

/**
 * Utility for working with IntelliJ IDEA scratch files.
 * Scratch files are temporary files that persist across IDE sessions and are available across all projects.
 */
object ScratchFileUtils {
    
    /**
     * Checks if the given VirtualFile is a scratch file.
     * @param file The VirtualFile to check
     * @return true if the file is a scratch file, false otherwise
     */
    fun isScratchFile(file: VirtualFile): Boolean {
        return try {
            ScratchRootType.getInstance().containsFile(file) || 
            ScratchFileService.getInstance().getRootType(file) != null
        } catch (_: Exception) {
            // Fallback: check if the file path contains the scratch directory pattern
            isScratchFileByPath(file.path)
        }
    }
    
    /**
     * Checks if the given file path appears to be a scratch file based on path patterns.
     * This is a fallback method when the ScratchFileService API is not available.
     * @param filePath The file path to check
     * @return true if the path appears to be a scratch file, false otherwise
     */
    fun isScratchFileByPath(filePath: String): Boolean {
        val normalizedPath = filePath.replace('\\', '/')
        return normalizedPath.contains("/scratches/") || 
               normalizedPath.contains("/.idea/scratches/") ||
               normalizedPath.matches(Regex(".*/scratches/.*"))
    }
}