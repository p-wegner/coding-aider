package de.andrena.codingaider.executors.strategies

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.AiderOutputSummaryService
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.settings.CustomLlmProviderService

abstract class AiderExecutionStrategy(protected val project: Project) {
    abstract fun buildCommand(commandData: CommandData): List<String>
    abstract fun prepareEnvironment(processBuilder: ProcessBuilder, commandData: CommandData)
    abstract fun cleanupAfterExecution()
    fun buildCommonArgs(commandData: CommandData, settings: AiderSettings): List<String> {
        return buildList {
            // Handle model selection based on provider type
            if (commandData.llm.isNotEmpty()) {
                val customProvider = CustomLlmProviderService.Companion.getInstance().getProvider(commandData.llm)
                if (customProvider != null) {
                    add("--model")
                    add(customProvider.prefixedModelName)
                } else {
                    if (commandData.llm.startsWith("--")) {
                        add(commandData.llm)
                    } else {
                        add("--model")
                        add(commandData.llm)
                    }
                }
            }
            commandData.files.forEach { fileData ->
                val fileArgument = if (fileData.isReadOnly) "--read" else "--file"
                add(fileArgument)
                add(fileData.filePath)
            }
            if (commandData.useYesFlag) add("--yes")
            if (commandData.editFormat.isNotEmpty()) {
                add("--edit-format")
                add(commandData.editFormat)
            }
            if (!commandData.isShellMode) {
                add("--no-suggest-shell-commands")
                add("--no-pretty")
                add("--no-fancy-input")
                add("--no-detect-urls")
            }
            if (commandData.additionalArgs.isNotEmpty()) {
                addAll(commandData.additionalArgs.split(" "))
            }
            if (commandData.lintCmd.isNotEmpty()) {
                add("--lint-cmd")
                add(commandData.lintCmd)
            }
            if (commandData.deactivateRepoMap) {
                add("--map-tokens")
                add("0")
            }
            if (commandData.options.autoCommit != null) {
                if (commandData.options.autoCommit) {
                    add("--auto-commits")
                } else {
                    add("--no-auto-commits")
                }
            } else {
                when (settings.autoCommits) {
                    AiderSettings.AutoCommitSetting.ON -> add("--auto-commits")
                    AiderSettings.AutoCommitSetting.OFF -> add("--no-auto-commits")
                    AiderSettings.AutoCommitSetting.DEFAULT -> {} // Do nothing, use Aider's default
                }
            }
            if (commandData.options.dirtyCommits != null) {
                if (commandData.options.dirtyCommits) {
                    add("--dirty-commits")
                } else {
                    add("--no-dirty-commits")
                }
            } else {
                when (settings.dirtyCommits) {
                    AiderSettings.DirtyCommitSetting.ON -> add("--dirty-commits")
                    AiderSettings.DirtyCommitSetting.OFF -> add("--no-dirty-commits")
                    AiderSettings.DirtyCommitSetting.DEFAULT -> {} // Do nothing, use Aider's default
                }
            }
            if (settings.includeChangeContext) {
                add("--commit-prompt")
                add(getCommitPrompt())
            }
            if (commandData.sidecarMode) {
                return@buildList
            }
            when (commandData.aiderMode) {
                AiderMode.NORMAL -> {
                    add("-m")
                    if (settings.summarizedOutput) {
                        add(
                            project.service<AiderOutputSummaryService>()
                                .createPromptPrefix() + "\n" + commandData.message
                        )
                    } else {
                        add(commandData.message)
                    }
                }

                AiderMode.STRUCTURED -> {
                    add("-m")
                    add(project.service<AiderPlanService>().createAiderPlanSystemPrompt(commandData))
                }

                AiderMode.ARCHITECT -> {
                    add("-m")
                    add("/architect ${commandData.message}")
                }

                else -> {}
            }
        }
    }


}


private fun getCommitPrompt(): String {
    // the prompt in the Aider CLI https://github.com/paul-gauthier/aider/blob/main/aider/prompts.py extended with prompt context
    val basePrompt = """
        You are an expert software engineer.
        Review the provided context and diffs which are about to be committed to a git repo.
        Review the diffs carefully.
        Generate a commit message for those changes.
        The commit message MUST use the imperative tense.
        The commit message should be structured as follows: <type>: <description>
        Use these for <type>: fix, feat, build, chore, ci, docs, style, refactor, perf, test
        Reply with JUST the commit message, without quotes, comments, questions, etc!
    """.trimIndent()

    val extendedPrompt = """
        Additional to this commit message, the next lines after the message should include the prompt of the USER that led to the change 
        and the files (without content) that were given as context. Make sure the compact commit message is clearly separated from the additional information. Example:
        feat: Add a button to the login page
        
        USER: Create a button to allow users to login
        FILES: login.html, login.css
    """.trimIndent()

    return "$basePrompt\n$extendedPrompt"
}