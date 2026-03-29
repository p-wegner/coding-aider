package de.andrena.codingaider.executors.strategies

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.ApiKeyManager

class GithubCopilotCliExecutionStrategy(
    project: Project,
    private val settings: AiderSettings
) : AiderExecutionStrategy(project) {

    override fun buildCommand(commandData: CommandData): MutableList<String> {
        val ghPath = settings.ghExecutablePath.ifBlank { "gh" }
        // Use `--` to prevent `gh` from interpreting Copilot CLI flags as its own
        val command = mutableListOf(ghPath, "copilot", "--")

        command.addAll(listOf("-p", buildPrompt(commandData)))
        command.add("--allow-all-tools")
        command.add("--allow-all-paths")

        if (commandData.projectPath.isNotEmpty()) {
            command.addAll(listOf("--add-dir", commandData.projectPath))
        }

        // Use --model only when not in BYOK mode (BYOK uses COPILOT_MODEL env var)
        if (commandData.llm.isNotEmpty() && settings.copilotProviderBaseUrl.isEmpty()) {
            command.addAll(listOf("--model", commandData.llm))
        }

        command.add("--silent")

        return command
    }

    private fun buildPrompt(commandData: CommandData): String {
        val fileContext = if (commandData.files.isNotEmpty()) {
            val fileList = commandData.files.joinToString(", ") { it.filePath }
            "Files for context: $fileList\n\n"
        } else {
            ""
        }
        val baseMessage = when (commandData.aiderMode) {
            AiderMode.ARCHITECT -> "/architect ${commandData.message}"
            else -> commandData.message
        }
        // gh copilot CLI truncates -p arguments at the first real newline on Windows.
        // Replace all newlines with literal \n to preserve structure without truncation.
        return "$fileContext$baseMessage"
            .replace("\r\n", "\\n")
            .replace("\r", "")
            .replace("\n", "\\n")
    }

    override fun prepareEnvironment(processBuilder: ProcessBuilder, commandData: CommandData) {
        val env = processBuilder.environment()

        if (settings.copilotProviderBaseUrl.isNotEmpty()) {
            env["COPILOT_PROVIDER_BASE_URL"] = settings.copilotProviderBaseUrl
            if (settings.copilotProviderType.isNotEmpty()) {
                env["COPILOT_PROVIDER_TYPE"] = settings.copilotProviderType
            }
            ApiKeyManager.getApiKey(COPILOT_PROVIDER_API_KEY_STORE)?.let { apiKey ->
                env["COPILOT_PROVIDER_API_KEY"] = apiKey
            }
            if (commandData.llm.isNotEmpty()) {
                env["COPILOT_MODEL"] = commandData.llm
            }
        }
    }

    override fun cleanupAfterExecution() {
        // No cleanup needed for Copilot CLI execution
    }

    companion object {
        const val COPILOT_PROVIDER_API_KEY_STORE = "COPILOT_PROVIDER_API_KEY"
    }
}
