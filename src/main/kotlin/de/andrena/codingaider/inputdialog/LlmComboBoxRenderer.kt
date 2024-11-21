package de.andrena.codingaider.inputdialog

import com.intellij.openapi.ui.ComboBox
import de.andrena.codingaider.settings.LlmSelection
import de.andrena.codingaider.utils.ApiKeyChecker
import java.awt.Component
import javax.swing.*

class LlmComboBoxRenderer(private val apiKeyChecker: ApiKeyChecker, private val llmComboBox: ComboBox<LlmSelection>, private val llmOptions: Array<LlmSelection>) : DefaultListCellRenderer() {
    private val apiKeyStatus = mutableMapOf<String, Boolean>()

    init {
        // Initialize status checking in background
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
            llmOptions.forEach { llm ->
                apiKeyStatus[llm.name] = apiKeyChecker.getApiKeyForLlm(llm.name)?.let {
                    apiKeyChecker.isApiKeyAvailableForLlm(llm.name)
                } ?: true
            }
            // Trigger UI update
            SwingUtilities.invokeLater { llmComboBox.repaint() }
        }
    }

    override fun getListCellRendererComponent(
        list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
    ): Component {
        val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        if (component is JLabel && value is String) {
            val apiKey = apiKeyChecker.getApiKeyForLlm(value)
            if (apiKey != null && apiKeyStatus[value] == false) {
                icon = UIManager.getIcon("OptionPane.errorIcon")
                toolTipText =
                    "API key not found in default locations for $value. This may not be an error if you're using an alternative method to provide the key."
            } else {
                icon = null
                toolTipText = null
            }
        }
        return component
    }
}