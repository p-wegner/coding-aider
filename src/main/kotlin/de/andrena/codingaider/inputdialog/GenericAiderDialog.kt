package de.andrena.codingaider.inputdialog

import com.intellij.openapi.project.Project
import de.andrena.codingaider.cli.CliFactory
import de.andrena.codingaider.cli.CliInterface
import de.andrena.codingaider.cli.CliMode
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.command.GenericDialog
import de.andrena.codingaider.settings.AiderSpecificSettings
import de.andrena.codingaider.settings.ClaudeCodeSpecificSettings
import de.andrena.codingaider.settings.GenericCliSettings
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.DefaultApiKeyChecker
import java.util.regex.Pattern

/**
 * Generic implementation of the Aider dialog that works with multiple CLI tools.
 * This class wraps the existing AiderInputDialog and provides the GenericDialog interface.
 */
class GenericAiderDialog(
    private val project: Project,
    files: List<FileData>,
    initialText: String = "",
    private val apiKeyChecker: ApiKeyChecker = DefaultApiKeyChecker()
) : GenericDialog {
    
    private val underlyingDialog: AiderInputDialog
    private val genericSettings = GenericCliSettings.getInstance()
    
    init {
        // Create the underlying Aider dialog
        underlyingDialog = AiderInputDialog(project, files, initialText, apiKeyChecker)
        
        // Update the dialog title to reflect the current CLI tool
        val cliName = genericSettings.selectedCli
        updateDialogTitle(cliName)
        
        // Filter available modes based on CLI capabilities
        updateAvailableModes()
    }
    
    private fun updateDialogTitle(cliName: String) {
        underlyingDialog.title = "${cliName.replaceFirstChar { it.uppercase() }} Assistant"
    }
    
    private fun updateAvailableModes() {
        val currentCli = CliFactory.getCurrentCli()
        if (currentCli != null) {
            // For now, just use the default mode - we can extend this later
            // based on actual CLI capabilities
            underlyingDialog.selectMode(AiderMode.NORMAL)
        }
    }
    
    override fun getPrompt(): String {
        return underlyingDialog.getInputText()
    }
    
    override fun getModel(): String {
        return underlyingDialog.getLlm().name
    }
    
    override fun getYesFlag(): Boolean {
        return underlyingDialog.isYesFlagChecked()
    }
    
    override fun getAdditionalArguments(): Map<String, String> {
        val additionalArgs = underlyingDialog.getAdditionalArgs()
        return parseAdditionalArgs(additionalArgs)
    }
    
    override fun getFiles(): List<FileData> {
        return underlyingDialog.getAllFiles()
    }
    
    override fun getMode(): CliMode {
        return ModeMapper.fromAiderMode(underlyingDialog.selectedMode)
    }
    
    override fun getProject(): Project {
        return underlyingDialog.project
    }
    
    override fun showAndGet(): Boolean {
        return underlyingDialog.showAndGet()
    }
    
    override fun getCliSpecificOptions(): Map<String, Any> {
        val cliName = genericSettings.selectedCli
        val currentCli = CliFactory.getCli(cliName)
        
        return when (cliName.lowercase()) {
            "aider" -> getAiderSpecificOptions()
            "claude", "claude-code" -> getClaudeSpecificOptions()
            else -> emptyMap()
        }
    }
    
    override fun validateInput(): List<String> {
        val errors = mutableListOf<String>()
        
        // Validate prompt
        val prompt = getPrompt()
        if (prompt.isBlank() && getMode() != CliMode.SHELL) {
            errors.add("Prompt cannot be empty for shell mode")
        }
        
        // Validate model
        if (getModel().isBlank()) {
            errors.add("Model must be selected")
        }
        
        // Validate files
        val files = getFiles()
        if (files.isEmpty()) {
            errors.add("At least one file must be selected")
        }
        
        // Validate CLI-specific requirements
        val cliName = genericSettings.selectedCli
        val currentCli = CliFactory.getCli(cliName)
        if (currentCli != null) {
            val cliErrors = currentCli.validateCommandData(
                de.andrena.codingaider.cli.CommandDataConverter.fromGenericCommandData(
                    de.andrena.codingaider.cli.GenericCommandData(
                        prompt = prompt,
                        model = getModel(),
                        files = files.map { de.andrena.codingaider.command.FileData(it.filePath, it.isReadOnly) },
                        options = de.andrena.codingaider.cli.GenericCommandOptions(
                            useYesFlag = getYesFlag(),
                            additionalOptions = getAdditionalArguments()
                        ),
                        cliMode = getMode(),
                        projectPath = project.basePath ?: "",
                        workingDir = "",
                        additionalArgs = getAdditionalArguments()
                    )
                )
            )
            errors.addAll(cliErrors)
        }
        
        return errors
    }
    
    override fun getDialogTitle(): String {
        return underlyingDialog.title
    }
    
    override fun getCliToolName(): String {
        return genericSettings.selectedCli
    }
    
    private fun getAiderSpecificOptions(): Map<String, Any> {
        val aiderSettings = AiderSpecificSettings.getInstance()
        return mapOf(
            "edit_format" to aiderSettings.state.editFormat,
            "lint_command" to aiderSettings.state.lintCmd,
            "reasoning_effort" to aiderSettings.state.reasoningEffort,
            "deactivate_repo_map" to aiderSettings.state.deactivateRepoMap,
            "include_change_context" to aiderSettings.state.includeChangeContext,
            "sidecar_mode" to aiderSettings.state.useSidecarMode
        )
    }
    
    private fun getClaudeSpecificOptions(): Map<String, Any> {
        val claudeSettings = ClaudeCodeSpecificSettings.getInstance()
        return mapOf(
            "max_tokens" to claudeSettings.state.maxTokens,
            "temperature" to claudeSettings.state.temperature,
            "top_p" to claudeSettings.state.topP,
            "system_prompt" to claudeSettings.state.systemPrompt,
            "user_context" to claudeSettings.state.userContext
        )
    }
    
    private fun parseAdditionalArgs(argsString: String): Map<String, String> {
        if (argsString.isBlank()) return emptyMap()
        
        val args = mutableMapOf<String, String>()
        val pattern = Pattern.compile("--([\\w-]+)(?:\\s*=\\s*|\\s+)([^\\s]+)")
        val matcher = pattern.matcher(argsString)
        
        while (matcher.find()) {
            args[matcher.group(1)] = matcher.group(2)
        }
        
        return args
    }
}