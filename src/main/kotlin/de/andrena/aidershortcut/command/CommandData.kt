package de.andrena.aidershortcut.command

data class CommandData(
    val message: String,
    val useYesFlag: Boolean,
    val selectedCommand: String,
    val additionalArgs: String,
    val filePaths: List<String>,
    val readOnlyFiles: List<String>,
    val isShellMode: Boolean
)
