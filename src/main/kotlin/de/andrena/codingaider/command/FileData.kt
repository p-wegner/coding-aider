package de.andrena.codingaider.command

import java.io.File

data class FileData(
    val filePath: String,
    val isReadOnly: Boolean
) {
    companion object {
        private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "bmp", "svg", "ico")
        private const val MAX_FILE_SIZE_BYTES = 100 * 1024 // 100KB
    }

    fun shouldSkipTokenCount(): Boolean {
        val file = File(filePath)
        return isImage() || isTooBig(file)
    }

    private fun isImage(): Boolean {
        val extension = filePath.substringAfterLast('.', "").lowercase()
        return extension in IMAGE_EXTENSIONS
    }

    private fun isTooBig(file: File): Boolean {
        return file.exists() && file.length() > MAX_FILE_SIZE_BYTES
    }
}


