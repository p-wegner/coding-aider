package de.andrena.codingaider.settings.cli

import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import de.andrena.codingaider.settings.cli.CliType.*
import java.awt.event.ActionEvent
import javax.swing.ButtonGroup
import javax.swing.JPanel

/**
 * Panel for selecting and configuring the CLI tool
 */
class CliSelectionPanel {
    
    private val aiderRadioButton = JBRadioButton("Aider")
    private val claudeCodeRadioButton = JBRadioButton("Claude Code")
    private val geminiCliRadioButton = JBRadioButton("Gemini CLI")
    private val codexCliRadioButton = JBRadioButton("Codex CLI")
    
    private val buttonGroup = ButtonGroup()
    
    private val genericCliSettings = GenericCliSettings.getInstance()
    
    private val settingsChangeListeners = mutableListOf<() -> Unit>()
    
    init {
        buttonGroup.add(aiderRadioButton)
        buttonGroup.add(claudeCodeRadioButton)
        buttonGroup.add(geminiCliRadioButton)
        buttonGroup.add(codexCliRadioButton)
        
        // Set initial selection
        when (genericCliSettings.getSelectedCli()) {
            AIDER -> aiderRadioButton.isSelected = true
            CLAUDE_CODE -> claudeCodeRadioButton.isSelected = true
            GEMINI_CLI -> geminiCliRadioButton.isSelected = true
            CODEX_CLI -> codexCliRadioButton.isSelected = true
        }
        
        // Add action listeners
        val radioListener = { event: ActionEvent ->
            val newCliType = when (event.source) {
                aiderRadioButton -> AIDER
                claudeCodeRadioButton -> CLAUDE_CODE
                geminiCliRadioButton -> GEMINI_CLI
                codexCliRadioButton -> CODEX_CLI
                else -> AIDER
            }
            genericCliSettings.setSelectedCli(newCliType)
            notifySettingsChanged()
        }
        
        aiderRadioButton.addActionListener(radioListener)
        claudeCodeRadioButton.addActionListener(radioListener)
        geminiCliRadioButton.addActionListener(radioListener)
        codexCliRadioButton.addActionListener(radioListener)
        
        // Disable unsupported CLIs for now
        geminiCliRadioButton.isEnabled = false
        codexCliRadioButton.isEnabled = false
    }
    
    fun addSettingsChangeListener(listener: () -> Unit) {
        settingsChangeListeners.add(listener)
    }
    
    private fun notifySettingsChanged() {
        settingsChangeListeners.forEach { it() }
    }
    
    fun createPanel(): JPanel {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("CLI Tool:"), aiderRadioButton)
            .addComponentToRightColumn(claudeCodeRadioButton)
            .addComponentToRightColumn(geminiCliRadioButton)
            .addComponentToRightColumn(codexCliRadioButton)
            .addTooltip("Select the CLI tool to use for AI assistance")
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }
    
    fun apply() {
        // The settings are already applied in real-time through the action listeners
        // No need to do anything here
    }
    
    fun isModified(): Boolean {
        return when (genericCliSettings.getSelectedCli()) {
            AIDER -> !aiderRadioButton.isSelected
            CLAUDE_CODE -> !claudeCodeRadioButton.isSelected
            GEMINI_CLI -> !geminiCliRadioButton.isSelected
            CODEX_CLI -> !codexCliRadioButton.isSelected
        }
    }
    
    fun reset() {
        when (genericCliSettings.getSelectedCli()) {
            AIDER -> aiderRadioButton.isSelected = true
            CLAUDE_CODE -> claudeCodeRadioButton.isSelected = true
            GEMINI_CLI -> geminiCliRadioButton.isSelected = true
            CODEX_CLI -> codexCliRadioButton.isSelected = true
        }
    }
    
    fun dispose() {
        // Clean up listeners if needed
        settingsChangeListeners.clear()
    }
}