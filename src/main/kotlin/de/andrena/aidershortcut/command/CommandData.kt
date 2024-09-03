package de.andrena.aidershortcut.command

data class CommandData(
    val message: String,
    val useYesFlag: Boolean,
    val selectedCommand: String,
    val additionalArgs: String,
    val writeableFiles: List<String>, // Renamed from filePaths to writeableFiles
    val readOnlyFiles: List<String>,
    val isShellMode: Boolean
)
