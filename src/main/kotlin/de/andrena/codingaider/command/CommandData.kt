package de.andrena.codingaider.command

data class CommandData(
    val message: String,
    val useYesFlag: Boolean,
    val llm: String,
    val additionalArgs: String,
    val files: List<FileData>,
    val isShellMode: Boolean,
    val lintCmd: String
) {
}

