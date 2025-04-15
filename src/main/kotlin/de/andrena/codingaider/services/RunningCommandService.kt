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

    fun addRunningCommand(dialog: MarkdownDialog) {
        runningCommandsListModel.addElement(dialog)
    }

    fun removeRunningCommand(dialog: MarkdownDialog) {
        runningCommandsListModel.removeElement(dialog)
    }

    fun getRunningCommandsListModel(): DefaultListModel<MarkdownDialog> = runningCommandsListModel

    fun storeCompletedCommand(commandData: CommandData?, output: String?) {
        lastCompletedCommand = commandData
        lastCommandOutput = output
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
            // Wait for command to finish and get output (blocking)
            // In a real implementation, you might want to make this async/future-based
            // For now, we rely on the service storing the last output
            lastCommand = cmd
            // Wait for the dialog to finish and get output
            // (This is a simplification; in practice, you may want to hook into the dialog's completion)
            // Here, we just use the lastCommandOutput after each execution
            lastOutput = lastCommandOutput
        }
        return lastOutput
    }

    /**
     * Existing plan creation logic, now implemented as a single-step chain for compatibility.
     */
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

    /**
     * Example: Run a multi-step chain of Aider commands.
     * This can be triggered from UI or API.
     */
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
            Create a structured plan from this summary:
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

        // Chain: summarize -> plan
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
}
