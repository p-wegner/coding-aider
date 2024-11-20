package de.andrena.codingaider.inputdialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.DefaultApiKeyChecker
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.KeyEvent
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class AiderOptionsPanel(
    private val project: Project,
    private val apiKeyChecker: ApiKeyChecker = DefaultApiKeyChecker()
) : JPanel(GridBagLayout()) {
    
    val llmOptions = apiKeyChecker.getAllLlmOptions().toTypedArray()
    val llmComboBox = object : ComboBox<String>(llmOptions) {
        override fun getToolTipText(): String? {
            return null // TODO: Enable this tooltip when slow thread error is fixed
        }
    }.apply {
        preferredSize = Dimension(150, preferredSize.height)
    }

    val yesCheckBox = JCheckBox("Add --yes flag").apply {
        toolTipText = "Automatically answer 'yes' to prompts"
        mnemonic = KeyEvent.VK_Y
    }

    val additionalArgsField = JTextField(20).apply {
        preferredSize = Dimension(200, preferredSize.height)
    }

    init {
        setupUI()
    }

    private fun setupUI() {
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.NONE
            anchor = GridBagConstraints.WEST
            insets = JBUI.insets(0, 2)
        }
        
        // LLM selection
        add(JBLabel("LLM:").apply {
            displayedMnemonic = KeyEvent.VK_L
            labelFor = llmComboBox
            toolTipText = "Select the Language Model to use"
        }, gbc.apply {
            gridx = 0
            gridy = 0
        })
        
        add(llmComboBox, gbc.apply {
            gridx = 1
            gridy = 0
            weightx = 0.3
        })
        
        // Yes flag
        add(yesCheckBox, gbc.apply {
            gridx = 2
            gridy = 0
            insets.left = 10
        })
        
        // Additional args
        add(JBLabel("Args:").apply {
            displayedMnemonic = KeyEvent.VK_A
            labelFor = additionalArgsField
            toolTipText = "Additional arguments for the Aider command"
        }, gbc.apply {
            gridx = 3
            gridy = 0
            insets.left = 10
        })
        
        add(additionalArgsField, gbc.apply {
            gridx = 4
            gridy = 0
            weightx = 0.7
            fill = GridBagConstraints.HORIZONTAL
        })
    }
}
