package de.andrena.aidershortcut

data class CommandData(
    val message: String,
    val useYesFlag: Boolean,
    val selectedCommand: String,
    val additionalArgs: String,
    val filePaths: String,
    val readOnlyFiles: List<String>,
    val isShellMode: Boolean
)
