package de.andrena.aidershortcut.command

data class CommandData(
    val message: String,
    val useYesFlag: Boolean,
    val selectedCommand: String,
    val additionalArgs: String,
    val files: List<FileData>,
    val isShellMode: Boolean
)

data class FileData(
    val filePath: String,
    val isReadOnly: Boolean
)
