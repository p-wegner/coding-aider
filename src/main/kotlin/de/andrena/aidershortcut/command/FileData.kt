package de.andrena.aidershortcut.command

data class FileData(
    val filePath: String,
    val isReadOnly: Boolean
)package de.andrena.aidershortcut.utils

import com.intellij.openapi.vfs.VirtualFile
import de.andrena.aidershortcut.command.FileData

object FileTraversal {
    fun traverseFileOrDirectory(file: VirtualFile, isReadOnly: Boolean = false): List<FileData> {
        return if (file.isDirectory) {
            file.children.flatMap { traverseFileOrDirectory(it, isReadOnly) }
        } else {
            listOf(FileData(file.path, isReadOnly))
        }
    }

    fun traverseFilesOrDirectories(files: Array<VirtualFile>, isReadOnly: Boolean = false): List<FileData> {
        return files.flatMap { file ->
            traverseFileOrDirectory(file, isReadOnly)
        }
    }
}
