package de.andrena.codingaider.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import de.andrena.codingaider.cli.CliFactory
import de.andrena.codingaider.cli.CliInfo
import org.jetbrains.annotations.Nls
import javax.swing.*

/**
 * Configurable for generic CLI settings.
 * This provides a unified settings interface for different CLI tools.
 */
class GenericCliSettingsConfigurable : Configurable {
    
    private var settingsPanel: GenericCliSettingsPanel? = null
    
    override fun getDisplayName(): @Nls String {
        return "Coding Assistant Settings"
    }
    
    override fun getHelpTopic(): String? {
        return "coding-aider.settings"
    }
    
    override fun createComponent(): JComponent {
        if (settingsPanel == null) {
            settingsPanel = GenericCliSettingsPanel()
        }
        return settingsPanel!!.panel
    }
    
    override fun isModified(): Boolean {
        return settingsPanel?.isModified() ?: false
    }
    
    override fun apply() {
        settingsPanel?.apply()
    }
    
    override fun reset() {
        settingsPanel?.reset()
    }
    
    override fun disposeUIResources() {
        settingsPanel?.dispose()
        settingsPanel = null
    }
}

/**
 * Panel for generic CLI settings.
 * This provides UI components for configuring different CLI tools.
 */
class GenericCliSettingsPanel {
    
    val panel: JPanel
    private val cliComboBox: JComboBox<String>
    private val settingsTabbedPane: JTabbedPane
    private val commonSettingsPanel: JPanel
    private val cliSpecificPanel: JPanel
    
    private var currentCliSettings: CliSpecificSettingsPanel? = null
    
    init {
        panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        
        // CLI Selection Panel
        val cliSelectionPanel = JPanel()
        cliSelectionPanel.layout = BoxLayout(cliSelectionPanel, BoxLayout.X_AXIS)
        
        cliSelectionPanel.add(JLabel("CLI Tool:"))
        cliComboBox = JComboBox(CliFactory.getSupportedCliTools().toTypedArray())
        cliSelectionPanel.add(cliComboBox)
        
        panel.add(cliSelectionPanel)
        panel.add(Box.createVerticalStrut(10))
        
        // Settings Tabs
        settingsTabbedPane = JTabbedPane()
        
        // Common Settings Tab
        commonSettingsPanel = createCommonSettingsPanel()
        settingsTabbedPane.addTab("Common Settings", commonSettingsPanel)
        
        // CLI-Specific Settings Tab
        cliSpecificPanel = JPanel()
        cliSpecificPanel.layout = BoxLayout(cliSpecificPanel, BoxLayout.Y_AXIS)
        settingsTabbedPane.addTab("CLI-Specific Settings", cliSpecificPanel)
        
        panel.add(settingsTabbedPane)
        
        // Add listener for CLI selection changes
        cliComboBox.addActionListener {
            updateCliSpecificSettings()
        }
        
        // Initialize with current settings
        reset()
    }
    
    private fun createCommonSettingsPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        
        val settings = GenericCliSettings.getInstance()
        
        // Default Model
        val modelPanel = JPanel()
        modelPanel.layout = BoxLayout(modelPanel, BoxLayout.X_AXIS)
        modelPanel.add(JLabel("Default Model:"))
        val modelField = JTextField(settings.defaultModel, 30)
        modelPanel.add(modelField)
        panel.add(modelPanel)
        
        // Default Mode
        val modePanel = JPanel()
        modePanel.layout = BoxLayout(modePanel, BoxLayout.X_AXIS)
        modePanel.add(JLabel("Default Mode:"))
        val modeComboBox = JComboBox(
            de.andrena.codingaider.cli.CliMode.values().map { it.displayName }.toTypedArray()
        )
        modePanel.add(modeComboBox)
        panel.add(modePanel)
        
        // Use Docker
        val dockerPanel = JPanel()
        dockerPanel.layout = BoxLayout(dockerPanel, BoxLayout.X_AXIS)
        val dockerCheckBox = JCheckBox("Use Docker")
        dockerCheckBox.isSelected = settings.commonExecutionOptions.useDocker
        dockerPanel.add(dockerCheckBox)
        panel.add(dockerPanel)
        
        // Additional Args
        val argsPanel = JPanel()
        argsPanel.layout = BoxLayout(argsPanel, BoxLayout.X_AXIS)
        argsPanel.add(JLabel("Additional Arguments:"))
        val argsField = JTextField(settings.commonExecutionOptions.additionalArgs, 30)
        argsPanel.add(argsField)
        panel.add(argsPanel)
        
        // Store references for apply/reset
        panel.putClientProperty("modelField", modelField)
        panel.putClientProperty("modeComboBox", modeComboBox)
        panel.putClientProperty("dockerCheckBox", dockerCheckBox)
        panel.putClientProperty("argsField", argsField)
        
        return panel
    }
    
    private fun updateCliSpecificSettings() {
        val selectedCli = cliComboBox.selectedItem as String
        val newPanel = createCliSpecificSettingsPanel(selectedCli)
        
        // Remove old panel
        cliSpecificPanel.removeAll()
        
        // Add new panel
        currentCliSettings = newPanel
        cliSpecificPanel.add(newPanel.panel)
        
        cliSpecificPanel.revalidate()
        cliSpecificPanel.repaint()
    }
    
    private fun createCliSpecificSettingsPanel(cliName: String): CliSpecificSettingsPanel {
        return when (cliName.lowercase()) {
            "aider" -> AiderSettingsPanel()
            "claude", "claude-code" -> ClaudeCodeSettingsPanel()
            else -> DefaultSettingsPanel(cliName)
        }
    }
    
    fun isModified(): Boolean {
        val settings = GenericCliSettings.getInstance()
        
        // Check CLI selection
        if (cliComboBox.selectedItem as String != settings.selectedCli) {
            return true
        }
        
        // Check common settings
        val commonPanel = commonSettingsPanel
        val modelField = commonPanel.getClientProperty("modelField") as JTextField
        val modeComboBox = commonPanel.getClientProperty("modeComboBox") as JComboBox<*>
        val dockerCheckBox = commonPanel.getClientProperty("dockerCheckBox") as JCheckBox
        val argsField = commonPanel.getClientProperty("argsField") as JTextField
        
        if (modelField.text != settings.defaultModel) return true
        if ((modeComboBox.selectedItem as String) != settings.defaultMode) return true
        if (dockerCheckBox.isSelected != settings.commonExecutionOptions.useDocker) return true
        if (argsField.text != settings.commonExecutionOptions.additionalArgs) return true
        
        // Check CLI-specific settings
        currentCliSettings?.let {
            if (it.isModified()) return true
        }
        
        return false
    }
    
    fun apply() {
        val settings = GenericCliSettings.getInstance()
        
        // Apply CLI selection
        settings.selectedCli = cliComboBox.selectedItem as String
        
        // Apply common settings
        val commonPanel = commonSettingsPanel
        val modelField = commonPanel.getClientProperty("modelField") as JTextField
        val modeComboBox = commonPanel.getClientProperty("modeComboBox") as JComboBox<*>
        val dockerCheckBox = commonPanel.getClientProperty("dockerCheckBox") as JCheckBox
        val argsField = commonPanel.getClientProperty("argsField") as JTextField
        
        settings.defaultModel = modelField.text
        settings.defaultMode = modeComboBox.selectedItem as String
        settings.commonExecutionOptions.useDocker = dockerCheckBox.isSelected
        settings.commonExecutionOptions.additionalArgs = argsField.text
        
        // Apply CLI-specific settings
        currentCliSettings?.apply()
        
        settings.notifySettingsChanged()
    }
    
    fun reset() {
        val settings = GenericCliSettings.getInstance()
        
        // Reset CLI selection
        cliComboBox.selectedItem = settings.selectedCli
        
        // Reset common settings
        val commonPanel = commonSettingsPanel
        val modelField = commonPanel.getClientProperty("modelField") as JTextField
        val modeComboBox = commonPanel.getClientProperty("modeComboBox") as JComboBox<*>
        val dockerCheckBox = commonPanel.getClientProperty("dockerCheckBox") as JCheckBox
        val argsField = commonPanel.getClientProperty("argsField") as JTextField
        
        modelField.text = settings.defaultModel
        modeComboBox.selectedItem = settings.defaultMode
        dockerCheckBox.isSelected = settings.commonExecutionOptions.useDocker
        argsField.text = settings.commonExecutionOptions.additionalArgs
        
        // Update CLI-specific settings
        updateCliSpecificSettings()
        
        // Reset CLI-specific settings
        currentCliSettings?.reset()
    }
    
    fun dispose() {
        currentCliSettings?.dispose()
    }
}

/**
 * Interface for CLI-specific settings panels.
 */
interface CliSpecificSettingsPanel {
    val panel: JPanel
    fun isModified(): Boolean
    fun apply()
    fun reset()
    fun dispose()
}

/**
 * Default settings panel for unsupported CLI tools.
 */
class DefaultSettingsPanel(private val cliName: String) : CliSpecificSettingsPanel {
    
    override val panel: JPanel = JPanel().apply {
        add(JLabel("No specific settings available for $cliName"))
    }
    
    override fun isModified(): Boolean = false
    
    override fun apply() {}
    
    override fun reset() {}
    
    override fun dispose() {}
}

/**
 * Aider-specific settings panel.
 */
class AiderSettingsPanel : CliSpecificSettingsPanel {
    
    override val panel: JPanel
    private val settings = AiderSpecificSettings.getInstance()
    
    // Form components
    private val executablePathField: JTextField
    private val reasoningEffortField: JTextField
    private val editFormatField: JTextField
    private val lintCmdField: JTextField
    private val deactivateRepoMapCheckBox: JCheckBox
    private val includeChangeContextCheckBox: JCheckBox
    private val useSidecarModeCheckBox: JCheckBox
    
    init {
        panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        
        // Executable Path
        val execPanel = createLabeledPanel("Aider Executable Path:", JTextField(settings.state.aiderExecutablePath, 30))
        executablePathField = execPanel.getComponent(1) as JTextField
        panel.add(execPanel)
        
        // Reasoning Effort
        val reasoningPanel = createLabeledPanel("Reasoning Effort:", JTextField(settings.state.reasoningEffort, 30))
        reasoningEffortField = reasoningPanel.getComponent(1) as JTextField
        panel.add(reasoningPanel)
        
        // Edit Format
        val editPanel = createLabeledPanel("Edit Format:", JTextField(settings.state.editFormat, 30))
        editFormatField = editPanel.getComponent(1) as JTextField
        panel.add(editPanel)
        
        // Lint Command
        val lintPanel = createLabeledPanel("Lint Command:", JTextField(settings.state.lintCmd, 30))
        lintCmdField = lintPanel.getComponent(1) as JTextField
        panel.add(lintPanel)
        
        // Checkboxes
        deactivateRepoMapCheckBox = JCheckBox("Deactivate Repository Map")
        deactivateRepoMapCheckBox.isSelected = settings.state.deactivateRepoMap
        panel.add(deactivateRepoMapCheckBox)
        
        includeChangeContextCheckBox = JCheckBox("Include Change Context")
        includeChangeContextCheckBox.isSelected = settings.state.includeChangeContext
        panel.add(includeChangeContextCheckBox)
        
        useSidecarModeCheckBox = JCheckBox("Use Sidecar Mode")
        useSidecarModeCheckBox.isSelected = settings.state.useSidecarMode
        panel.add(useSidecarModeCheckBox)
    }
    
    private fun createLabeledPanel(labelText: String, component: JComponent): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.add(JLabel(labelText))
        panel.add(component)
        return panel
    }
    
    override fun isModified(): Boolean {
        return executablePathField.text != settings.state.aiderExecutablePath ||
                reasoningEffortField.text != settings.state.reasoningEffort ||
                editFormatField.text != settings.state.editFormat ||
                lintCmdField.text != settings.state.lintCmd ||
                deactivateRepoMapCheckBox.isSelected != settings.state.deactivateRepoMap ||
                includeChangeContextCheckBox.isSelected != settings.state.includeChangeContext ||
                useSidecarModeCheckBox.isSelected != settings.state.useSidecarMode
    }
    
    override fun apply() {
        settings.state.aiderExecutablePath = executablePathField.text
        settings.state.reasoningEffort = reasoningEffortField.text
        settings.state.editFormat = editFormatField.text
        settings.state.lintCmd = lintCmdField.text
        settings.state.deactivateRepoMap = deactivateRepoMapCheckBox.isSelected
        settings.state.includeChangeContext = includeChangeContextCheckBox.isSelected
        settings.state.useSidecarMode = useSidecarModeCheckBox.isSelected
    }
    
    override fun reset() {
        executablePathField.text = settings.state.aiderExecutablePath
        reasoningEffortField.text = settings.state.reasoningEffort
        editFormatField.text = settings.state.editFormat
        lintCmdField.text = settings.state.lintCmd
        deactivateRepoMapCheckBox.isSelected = settings.state.deactivateRepoMap
        includeChangeContextCheckBox.isSelected = settings.state.includeChangeContext
        useSidecarModeCheckBox.isSelected = settings.state.useSidecarMode
    }
    
    override fun dispose() {
        // No resources to dispose
    }
}

/**
 * Claude Code-specific settings panel.
 */
class ClaudeCodeSettingsPanel : CliSpecificSettingsPanel {
    
    override val panel: JPanel
    private val settings = ClaudeCodeSpecificSettings.getInstance()
    
    // Form components
    private val executablePathField: JTextField
    private val maxTokensField: JTextField
    private val temperatureField: JTextField
    private val topPField: JTextField
    private val systemPromptField: JTextArea
    private val userContextField: JTextArea
    
    init {
        panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        
        // Executable Path
        val execPanel = createLabeledPanel("Claude Code Executable Path:", JTextField(settings.claudeExecutablePath, 30))
        executablePathField = execPanel.getComponent(1) as JTextField
        panel.add(execPanel)
        
        // Max Tokens
        val tokensPanel = createLabeledPanel("Max Tokens:", JTextField(settings.state.maxTokens.toString(), 30))
        maxTokensField = tokensPanel.getComponent(1) as JTextField
        panel.add(tokensPanel)
        
        // Temperature
        val tempPanel = createLabeledPanel("Temperature:", JTextField(settings.state.temperature.toString(), 30))
        temperatureField = tempPanel.getComponent(1) as JTextField
        panel.add(tempPanel)
        
        // Top-P
        val topPPanel = createLabeledPanel("Top-P:", JTextField(settings.state.topP.toString(), 30))
        topPField = topPPanel.getComponent(1) as JTextField
        panel.add(topPPanel)
        
        // System Prompt
        val sysPanel = JPanel()
        sysPanel.layout = BoxLayout(sysPanel, BoxLayout.Y_AXIS)
        sysPanel.add(JLabel("System Prompt:"))
        systemPromptField = JTextArea(settings.state.systemPrompt, 5, 30)
        systemPromptField.lineWrap = true
        sysPanel.add(JScrollPane(systemPromptField))
        panel.add(sysPanel)
        
        // User Context
        val userPanel = JPanel()
        userPanel.layout = BoxLayout(userPanel, BoxLayout.Y_AXIS)
        userPanel.add(JLabel("User Context:"))
        userContextField = JTextArea(settings.state.userContext, 5, 30)
        userContextField.lineWrap = true
        userPanel.add(JScrollPane(userContextField))
        panel.add(userPanel)
    }
    
    private fun createLabeledPanel(labelText: String, component: JComponent): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.add(JLabel(labelText))
        panel.add(component)
        return panel
    }
    
    override fun isModified(): Boolean {
        return executablePathField.text != settings.state.claudeExecutablePath ||
                maxTokensField.text != settings.state.maxTokens.toString() ||
                temperatureField.text != settings.state.temperature.toString() ||
                topPField.text != settings.state.topP.toString() ||
                systemPromptField.text != settings.state.systemPrompt ||
                userContextField.text != settings.state.userContext
    }
    
    override fun apply() {
        settings.state.claudeExecutablePath = executablePathField.text
        settings.state.maxTokens = maxTokensField.text.toIntOrNull() ?: settings.state.maxTokens
        settings.state.temperature = temperatureField.text.toDoubleOrNull() ?: settings.state.temperature
        settings.state.topP = topPField.text.toDoubleOrNull() ?: settings.state.topP
        settings.state.systemPrompt = systemPromptField.text
        settings.state.userContext = userContextField.text
    }
    
    override fun reset() {
        executablePathField.text = settings.state.claudeExecutablePath
        maxTokensField.text = settings.state.maxTokens.toString()
        temperatureField.text = settings.state.temperature.toString()
        topPField.text = settings.state.topP.toString()
        systemPromptField.text = settings.state.systemPrompt
        userContextField.text = settings.state.userContext
    }
    
    override fun dispose() {
        // No resources to dispose
    }
}