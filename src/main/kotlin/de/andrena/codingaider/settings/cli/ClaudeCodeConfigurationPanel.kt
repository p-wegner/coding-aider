package de.andrena.codingaider.settings.cli

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import de.andrena.codingaider.settings.cli.ClaudeCodeSettings.Companion.getInstance
import java.awt.event.ItemEvent
import javax.swing.JPanel

/**
 * Panel for configuring Claude Code specific settings
 */
class ClaudeCodeConfigurationPanel {
    
    private val claudeCodeSettings = getInstance()
    
    private val executablePathField = JBTextField(claudeCodeSettings.claudeExecutablePath)
    private val defaultModelField = JBTextField(claudeCodeSettings.model)
    private val maxTokensField = JBTextField(claudeCodeSettings.maxTokens.toString())
    private val temperatureField = JBTextField(claudeCodeSettings.temperature.toString())
    private val useDockerCheckbox = JBCheckBox("Use Docker", claudeCodeSettings.useDocker)
    private val dockerImageField = JBTextField(claudeCodeSettings.dockerImage)
    private val additionalArgsField = JBTextField(claudeCodeSettings.additionalArgs)
    private val verboseLoggingCheckbox = JBCheckBox("Verbose Logging", claudeCodeSettings.verboseLogging)
    private val enableMcpServerCheckbox = JBCheckBox("Enable MCP Server", claudeCodeSettings.enableMcpServer)
    private val mcpServerPortField = JBTextField(claudeCodeSettings.mcpServerPort.toString())
    private val mcpServerAutoStartCheckbox = JBCheckBox("Auto-start MCP Server", claudeCodeSettings.mcpServerAutoStart)
    
    private val settingsChangeListeners = mutableListOf<() -> Unit>()
    
    init {
        // Change listeners are not needed - we track modifications in isModified()
    }
    
    fun addSettingsChangeListener(listener: () -> Unit) {
        settingsChangeListeners.add(listener)
    }
    
    private fun notifySettingsChanged() {
        settingsChangeListeners.forEach { it() }
    }
    
    fun createPanel(): JPanel {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Executable Path:"), executablePathField)
            .addTooltip("Path to the Claude Code executable (default: 'claude')")
            .addLabeledComponent(JBLabel("Default Model:"), defaultModelField)
            .addTooltip("Default model to use (e.g., 'claude-3-sonnet-20240229')")
            .addLabeledComponent(JBLabel("Max Tokens:"), maxTokensField)
            .addTooltip("Maximum number of tokens for responses")
            .addLabeledComponent(JBLabel("Temperature:"), temperatureField)
            .addTooltip("Temperature setting for response randomness (0.0-2.0)")
            .addSeparator()
            .addComponent(JBLabel("Docker Configuration"))
            .addComponent(useDockerCheckbox)
            .addTooltip("Run Claude Code in a Docker container")
            .addLabeledComponent(JBLabel("Docker Image:"), dockerImageField)
            .addTooltip("Docker image to use for Claude Code")
            .addSeparator()
            .addComponent(JBLabel("Additional Settings"))
            .addLabeledComponent(JBLabel("Additional Args:"), additionalArgsField)
            .addTooltip("Additional command-line arguments to pass to Claude Code")
            .addComponent(verboseLoggingCheckbox)
            .addTooltip("Enable verbose logging for debugging")
            .addSeparator()
            .addComponent(JBLabel("MCP Server Configuration"))
            .addComponent(enableMcpServerCheckbox)
            .addTooltip("Enable MCP server for persistent file management")
            .addLabeledComponent(JBLabel("MCP Server Port:"), mcpServerPortField)
            .addTooltip("Port for MCP server (default: 8081)")
            .addComponent(mcpServerAutoStartCheckbox)
            .addTooltip("Auto-start MCP server when plugin starts")
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }
    
    fun apply() {
        claudeCodeSettings.claudeExecutablePath = executablePathField.text
        claudeCodeSettings.model = defaultModelField.text
        
        try {
            claudeCodeSettings.maxTokens = maxTokensField.text.toInt()
        } catch (e: NumberFormatException) {
            // Keep existing value if invalid
        }
        
        try {
            claudeCodeSettings.temperature = temperatureField.text.toDouble()
        } catch (e: NumberFormatException) {
            // Keep existing value if invalid
        }
        
        claudeCodeSettings.useDocker = useDockerCheckbox.isSelected
        claudeCodeSettings.dockerImage = dockerImageField.text
        claudeCodeSettings.additionalArgs = additionalArgsField.text
        claudeCodeSettings.verboseLogging = verboseLoggingCheckbox.isSelected
        
        claudeCodeSettings.enableMcpServer = enableMcpServerCheckbox.isSelected
        claudeCodeSettings.mcpServerAutoStart = mcpServerAutoStartCheckbox.isSelected
        
        try {
            claudeCodeSettings.mcpServerPort = mcpServerPortField.text.toInt()
        } catch (e: NumberFormatException) {
            // Keep existing value if invalid
        }
    }
    
    fun reset() {
        executablePathField.text = claudeCodeSettings.claudeExecutablePath
        defaultModelField.text = claudeCodeSettings.model
        maxTokensField.text = claudeCodeSettings.maxTokens.toString()
        temperatureField.text = claudeCodeSettings.temperature.toString()
        useDockerCheckbox.isSelected = claudeCodeSettings.useDocker
        dockerImageField.text = claudeCodeSettings.dockerImage
        additionalArgsField.text = claudeCodeSettings.additionalArgs
        verboseLoggingCheckbox.isSelected = claudeCodeSettings.verboseLogging
        enableMcpServerCheckbox.isSelected = claudeCodeSettings.enableMcpServer
        mcpServerPortField.text = claudeCodeSettings.mcpServerPort.toString()
        mcpServerAutoStartCheckbox.isSelected = claudeCodeSettings.mcpServerAutoStart
    }
    
    fun isModified(): Boolean {
        return executablePathField.text != claudeCodeSettings.claudeExecutablePath ||
                defaultModelField.text != claudeCodeSettings.model ||
                maxTokensField.text != claudeCodeSettings.maxTokens.toString() ||
                temperatureField.text != claudeCodeSettings.temperature.toString() ||
                useDockerCheckbox.isSelected != claudeCodeSettings.useDocker ||
                dockerImageField.text != claudeCodeSettings.dockerImage ||
                additionalArgsField.text != claudeCodeSettings.additionalArgs ||
                verboseLoggingCheckbox.isSelected != claudeCodeSettings.verboseLogging ||
                enableMcpServerCheckbox.isSelected != claudeCodeSettings.enableMcpServer ||
                mcpServerPortField.text != claudeCodeSettings.mcpServerPort.toString() ||
                mcpServerAutoStartCheckbox.isSelected != claudeCodeSettings.mcpServerAutoStart
    }
}