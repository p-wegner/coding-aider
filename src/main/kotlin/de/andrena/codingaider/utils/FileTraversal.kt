package de.andrena.codingaider.utils

import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.command.FileData

object FileTraversal {
    private fun traverseFileOrDirectory(file: VirtualFile, isReadOnly: Boolean = false): List<FileData> {
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