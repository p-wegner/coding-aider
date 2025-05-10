package de.andrena.codingaider.settings

import de.andrena.codingaider.utils.ApiKeyChecker
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.UIManager

/**
 * Custom renderer for LLM selection combo boxes
 */
class LlmComboBoxRenderer(private val apiKeyChecker: ApiKeyChecker) : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        if (component is JLabel && value is LlmSelection) {
            text = value.getDisplayText().ifBlank { "" }

            when {
                value.provider != null -> {
                    // Icon handling removed for simplicity
                    toolTipText = "Custom ${value.provider.type.displayName} provider: ${value.provider.name}"
                }

                !value.isBuiltIn -> {
                    val apiKey = apiKeyChecker.getApiKeyForLlm(value.name)
                    if (apiKey != null && !apiKeyChecker.isApiKeyAvailableForLlm(value.name)) {
                        icon = UIManager.getIcon("OptionPane.errorIcon")
                        toolTipText = "API key not found in default locations for ${value.name}"
                    } else {
                        icon = null
                        toolTipText = null
                    }
                }
            }
        }
        return component
    }
}
