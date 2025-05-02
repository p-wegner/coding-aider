package de.andrena.codingaider.services

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import javax.swing.JOptionPane

class ExampleChainCommand {
    // TODO: refactor this to use it for an action that opens a dialog with a text area,
    //  runs the aider command with the provided text as message and runs a summery command afterwards
    /**
     * Opens a dialog with a text area for the user to enter a prompt, runs the aider command with the provided text,
     * and then runs a summary command on the output. The summary is shown in a new dialog.
     *
     * @param project The current project
     * @param files The files to include in the context for the aider command
     * @param dialogTitle The title for the input dialog
     * @param initialPrompt Optional initial prompt text
     */
    fun runPromptAndSummarize(
        project: Project,
        files: List<de.andrena.codingaider.command.FileData>,
        dialogTitle: String = "Aider Prompt and Summarize",
        initialPrompt: String = ""
    ) {
        val promptArea = javax.swing.JTextArea(initialPrompt, 10, 60).apply {
            lineWrap = true
            wrapStyleWord = true
            font = javax.swing.UIManager.getFont("TextField.font")
        }
        val result = JOptionPane.showConfirmDialog(
            null,
            javax.swing.JScrollPane(promptArea),
            dialogTitle,
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        )
        if (result != JOptionPane.OK_OPTION) return

        val userPrompt = promptArea.text.trim()
        if (userPrompt.isBlank()) return

        val settings = de.andrena.codingaider.settings.AiderSettings.getInstance()
        val commandData = CommandData(
            message = userPrompt,
            useYesFlag = settings.useYesFlag,
            llm = settings.llm,
            additionalArgs = settings.additionalArgs,
            files = files,
            lintCmd = settings.lintCmd,
            deactivateRepoMap = settings.deactivateRepoMap,
            editFormat = settings.editFormat,
            projectPath = project.basePath ?: "",
            sidecarMode = settings.useSidecarMode
        )

        val executor = IDEBasedExecutor(project, commandData)
        val dialog = executor.execute()
        executor.isFinished().await()
        val output = dialog.toString()

        val summaryCommand = commandData.copy(
            message = "Summarize the following output:\n$output",
            files = commandData.files
        )
        val summaryExecutor = IDEBasedExecutor(project, summaryCommand)
        summaryExecutor.execute()
    }
}