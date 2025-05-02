package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.outputview.MarkdownDialog
import javax.swing.DefaultListModel
import javax.swing.JOptionPane

@Service(Service.Level.PROJECT)
class RunningCommandService {
    private val runningCommandsListModel = DefaultListModel<MarkdownDialog>()
    private var lastCompletedCommand: CommandData? = null
    private var lastCommandOutput: String? = null

    // Store commit hashes for last aider command
    private var lastAiderCommitBefore: String? = null
    private var lastAiderCommitAfter: String? = null

    fun addRunningCommand(dialog: MarkdownDialog) {
        runningCommandsListModel.addElement(dialog)
    }

    fun removeRunningCommand(dialog: MarkdownDialog) {
        runningCommandsListModel.removeElement(dialog)
    }

    fun getRunningCommandsListModel(): DefaultListModel<MarkdownDialog> = runningCommandsListModel

    fun storeCompletedCommand(commandData: CommandData?, output: String?, commitBefore: String? = null, commitAfter: String? = null) {
        lastCompletedCommand = commandData
        lastCommandOutput = output
        if (commitBefore != null && commitAfter != null) {
            lastAiderCommitBefore = commitBefore
            lastAiderCommitAfter = commitAfter
        }
    }

    fun getLastAiderCommitHashes(): Pair<String?, String?>? {
        return if (lastAiderCommitBefore != null && lastAiderCommitAfter != null) {
            Pair(lastAiderCommitBefore, lastAiderCommitAfter)
        } else {
            null
        }
    }

    /**
     * Data class representing a single step in a multi-step Aider command chain.
     * Each step can optionally take the previous output as input.
     */
    data class ChainedAiderCommand(
        val commandData: CommandData,
        val transformOutputToInput: ((String) -> String)? = null // If set, will use previous output as input
    )

    /**
     * Execute a chain of Aider commands, where each command can use the output of the previous as input.
     * The chain is executed synchronously, and each step's output is passed to the next step if requested.
     * The last output is returned.
     */
    fun executeChainedCommands(
        project: Project,
        commands: List<ChainedAiderCommand>
    ): String? {
        var lastOutput: String? = null
        var lastCommand: CommandData? = null
        for ((index, chained) in commands.withIndex()) {
            val cmd = if (chained.transformOutputToInput != null && lastOutput != null) {
                chained.commandData.copy(
                    message = chained.transformOutputToInput.invoke(lastOutput)
                )
            } else {
                chained.commandData
            }
            val executor = IDEBasedExecutor(project, cmd)
            executor.execute()
            // Block until the dialog signals completion
            executor.isFinished().await()
            // After dialog is finished, use the lastCommandOutput
            lastCommand = cmd
            lastOutput = lastCommandOutput
        }
        return lastOutput
    }

    fun createPlanFromLastCommand(project: Project) {
        if (lastCompletedCommand == null || lastCommandOutput == null) {
            JOptionPane.showMessageDialog(
                null,
                "No completed command available to create a plan from.",
                "Create Plan",
                JOptionPane.INFORMATION_MESSAGE
            )
            return
        }

        try {
            val command = lastCompletedCommand!!
            val output = lastCommandOutput!!

            // Create structured mode command data with all relevant files
            val planCommand = command.copy(
                message = """
                    Create a structured plan from this command output:
                    Command: ${command.message}
                    Output:
                    $output

                    Include:
                    1. Original command context
                    2. Implementation steps from output
                    3. Any follow-up tasks identified
                """.trimIndent(),
                aiderMode = AiderMode.STRUCTURED,
                files = command.files,
                options = command.options.copy(
                    disablePresentation = false,
                    autoCloseDelay = 10
                )
            )

            // Use the new chaining mechanism for a single-step chain
            executeChainedCommands(
                project,
                listOf(ChainedAiderCommand(planCommand))
            )

        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                null,
                "Error during plan creation: ${e.message}",
                "Plan Creation Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    // TODO: refactor this to use it for an action that opens a dialog with a text area, runs the aider command with the provided text as message and runs a summery command afterwards, similar to documentCodeAction
    fun runExampleMultiStepChain(project: Project) {
        if (lastCompletedCommand == null || lastCommandOutput == null) {
            JOptionPane.showMessageDialog(
                null,
                "No completed command available to create a chain from.",
                "Create Command Chain",
                JOptionPane.INFORMATION_MESSAGE
            )
            return
        }
        val command = lastCompletedCommand!!
        val output = lastCommandOutput!!

        // Step 1: Summarize the output
        val summarizeCommand = command.copy(
            message = "Summarize the following output:\n$output",
            aiderMode = command.aiderMode,
            files = command.files
        )
        // Step 2: Create a plan from the summary
        val planMessageBuilder: (String) -> String = { summary: String ->
            """
            Create a summary of these changes:
            $summary

            Include:
            1. Original command context
            2. Implementation steps from summary
            3. Any follow-up tasks identified
            """.trimIndent()
        }
        val planCommand = command.copy(
            message = "", // Placeholder, will be set by transformOutputToInput
            aiderMode = AiderMode.STRUCTURED,
            files = command.files,
            options = command.options.copy(
                disablePresentation = false,
                autoCloseDelay = 10
            )
        )

        executeChainedCommands(
            project,
            listOf(
                ChainedAiderCommand(summarizeCommand),
                ChainedAiderCommand(
                    planCommand,
                    transformOutputToInput = planMessageBuilder
                )
            )
        )
    }

    fun hasCompletedCommand(): Boolean = lastCompletedCommand != null && lastCommandOutput != null

    /**
     * Opens a dialog with a text area for the user to enter a prompt, runs the aider command with the provided text,
     * and then runs a summary command on the output. The summary is shown in a new dialog.
     *
     * @param project The current project
     * @param files The files to include in the context for the aider command
     * @param dialogTitle The title for the input dialog
     * @param summaryTitle The title for the summary dialog
     * @param initialPrompt Optional initial prompt text
     */
    fun runPromptAndSummarize(
        project: Project,
        files: List<de.andrena.codingaider.command.FileData>,
        dialogTitle: String = "Aider Prompt and Summarize",
        summaryTitle: String = "Aider Summary",
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
        // Wait for completion
        executor.isFinished().await()
        val output = dialog.toString()

        // Now run the summary command
        val summaryCommand = commandData.copy(
            message = "Summarize the following output:\n$output",
            files = commandData.files
        )
        val summaryExecutor = IDEBasedExecutor(project, summaryCommand)
        summaryExecutor.execute()
    }
}
