package de.andrena.codingaider.inputdialog

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.commandhistory.AiderHistoryHandler
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.ApiKeyChecker
import java.awt.BorderLayout
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.KeyEvent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.*

class AiderInputDialog(
    project: Project,
    files: List<FileData>
) : DialogWrapper(project) {
    private val settings = AiderSettings.getInstance(project)
    private val inputTextArea = JTextArea(5, 30)
    private val yesCheckBox = JCheckBox("Add --yes flag", settings.useYesFlag)
    private val llmOptions = ApiKeyChecker.getAllLlmOptions().toTypedArray()
    private val llmComboBox = object : ComboBox<String>(llmOptions) {
        override fun getToolTipText(): String? {
            val selectedItem = selectedItem as? String ?: return null
            return if (ApiKeyChecker.isApiKeyAvailableForLlm(selectedItem)) {
                "API key found for $selectedItem"
            } else {
                "API key not found for $selectedItem"
            }
        }
    }
    private val additionalArgsField = JTextField(settings.additionalArgs, 20)
    private val modeToggle = JCheckBox("Shell Mode", settings.isShellMode)
    private val messageLabel = JLabel("Enter your message:")
    private val historyComboBox = ComboBox<HistoryItem>()
    private val historyHandler = AiderHistoryHandler(project.basePath ?: "")
    private val aiderContextView: AiderContextView
    private val persistentFileManager: PersistentFileManager

    init {
        title = "Aider Command"
        persistentFileManager = PersistentFileManager(project.basePath ?: "")
        aiderContextView = AiderContextView(project, files + persistentFileManager.getPersistentFiles())
        init()
        loadHistory()
        setOKButtonText("OK")
        setCancelButtonText("Cancel")
        setupKeyBindings()
        llmComboBox.selectedItem = settings.llm
        llmComboBox.renderer = LlmComboBoxRenderer()
    }

    private fun setupKeyBindings() {
        val inputMap = inputTextArea.getInputMap(JComponent.WHEN_FOCUSED)
        val actionMap = inputTextArea.actionMap

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.ALT_DOWN_MASK), "previousHistory")
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.ALT_DOWN_MASK), "nextHistory")

        actionMap.put("previousHistory", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                navigateHistory(-1)
            }
        })

        actionMap.put("nextHistory", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                navigateHistory(1)
            }
        })
    }

    private fun navigateHistory(direction: Int) {
        val currentIndex = historyComboBox.selectedIndex
        val newIndex = (currentIndex + direction).coerceIn(0, historyComboBox.itemCount - 1)
        if (newIndex != currentIndex) {
            historyComboBox.selectedIndex = newIndex
            val selectedItem = historyComboBox.selectedItem as? HistoryItem
            inputTextArea.text = selectedItem?.command?.joinToString("\n") ?: ""
        }
    }

    override fun createActions(): Array<Action> {
        val actions = super.createActions()
        (actions[0] as? DialogWrapperAction)?.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_O)
        (actions[1] as? DialogWrapperAction)?.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C)
        return actions
    }

    override fun show() {
        super.show()
        SwingUtilities.invokeLater {
            inputTextArea.requestFocusInWindow()
            inputTextArea.caretPosition = inputTextArea.document.length
        }
    }

    private fun loadHistory() {
        historyComboBox.addItem(HistoryItem(emptyList(), null))  // Empty entry
        historyHandler.getHistory().forEach { (dateTime, command) ->
            historyComboBox.addItem(HistoryItem(command, dateTime))
        }
        historyComboBox.renderer = HistoryItemRenderer()
        historyComboBox.selectedIndex = 0  // Select the empty entry initially
        historyComboBox.addActionListener {
            if (historyComboBox.selectedIndex > 0 && historyComboBox.selectedItem is HistoryItem) {
                val selectedItem = historyComboBox.selectedItem as HistoryItem
                inputTextArea.text = selectedItem.command.joinToString("\n")
            } else {
                inputTextArea.text = ""  // Clear the input area when empty entry is selected
            }
        }
    }

    private data class HistoryItem(val command: List<String>, val dateTime: LocalDateTime?)

    private inner class HistoryItemRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (value is HistoryItem) {
                text = value.command.firstOrNull() ?: ""
                if (value.dateTime != null) {
                    val formattedDate = value.dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    val fullCommand = value.command.joinToString("\n")
                    toolTipText = "<html>$formattedDate<br><pre>$fullCommand</pre></html>"
                } else {
                    toolTipText = null
                }
            }
            return component
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(10)

        val topPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = JBUI.insets(5)
            weightx = 1.0
            weighty = 0.0
            gridx = 0
            gridy = 0
            gridwidth = GridBagConstraints.REMAINDER
        }

        modeToggle.mnemonic = KeyEvent.VK_M
        topPanel.add(modeToggle, gbc)
        gbc.gridy++
        val historyLabel = JLabel("Command History:")
        historyLabel.displayedMnemonic = KeyEvent.VK_H
        historyLabel.labelFor = historyComboBox
        topPanel.add(historyLabel, gbc)
        gbc.gridy++
        topPanel.add(historyComboBox, gbc)
        gbc.gridy++
        messageLabel.displayedMnemonic = KeyEvent.VK_E
        messageLabel.labelFor = inputTextArea
        topPanel.add(messageLabel, gbc)
        gbc.gridy++
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        topPanel.add(JBScrollPane(inputTextArea), gbc)
        gbc.gridy++
        gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        yesCheckBox.mnemonic = KeyEvent.VK_Y
        topPanel.add(yesCheckBox, gbc)
        gbc.gridy++
        gbc.gridwidth = 1
        val selectLlmLabel = JLabel("Select LLM:")
        selectLlmLabel.displayedMnemonic = KeyEvent.VK_L
        selectLlmLabel.labelFor = llmComboBox
        topPanel.add(selectLlmLabel, gbc)
        gbc.gridx = 1
        gbc.gridwidth = GridBagConstraints.REMAINDER
        topPanel.add(llmComboBox, gbc)
        gbc.gridy++
        gbc.gridx = 0
        gbc.gridwidth = 1
        val additionalArgsLabel = JLabel("Additional arguments:")
        additionalArgsLabel.displayedMnemonic = KeyEvent.VK_A
        additionalArgsLabel.labelFor = additionalArgsField
        topPanel.add(additionalArgsLabel, gbc)
        gbc.gridx = 1
        gbc.gridwidth = GridBagConstraints.REMAINDER
        topPanel.add(additionalArgsField, gbc)

        panel.add(topPanel, BorderLayout.NORTH)

        val actionGroup = DefaultActionGroup().apply {
            add(object : AnAction(
                "Toggle Read-Only Mode",
                "Toggle Read-Only Mode for selected file",
                AllIcons.Actions.Edit
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    aiderContextView.toggleReadOnlyMode()
                }
            })
            add(object : AnAction(
                "Toggle Persistent Files",
                "Toggle selected files' persistent status",
                AllIcons.Actions.MenuSaveall
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    aiderContextView.togglePersistentFile()
                }
            })
        }

        val toolbar = ActionManager.getInstance().createActionToolbar("AiderContextToolbar", actionGroup, true)
        toolbar.targetComponent = aiderContextView
        val contextPanel = JPanel(BorderLayout()).apply {
            add(toolbar.component, BorderLayout.NORTH)
            add(aiderContextView, BorderLayout.CENTER)
        }

        panel.add(contextPanel, BorderLayout.CENTER)

        modeToggle.addActionListener {
            val isShellMode = modeToggle.isSelected
            inputTextArea.isVisible = !isShellMode
            messageLabel.text = if (isShellMode) "Shell mode enabled" else "Enter your message:"
        }

        // Set focus on the input text area when the dialog is opened
        inputTextArea.requestFocusInWindow()

        return panel
    }

    fun getInputText(): String = inputTextArea.text
    fun isYesFlagChecked(): Boolean = yesCheckBox.isSelected
    fun getLlm(): String = llmComboBox.selectedItem as String
    fun getAdditionalArgs(): String = additionalArgsField.text
    fun getAllFiles(): List<FileData> = aiderContextView.getAllFiles()
    fun isShellMode(): Boolean = modeToggle.isSelected

    private inner class LlmComboBoxRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (component is JLabel && value is String) {
                val apiKey = ApiKeyChecker.getApiKeyForLlm(value)
                if (apiKey != null) {
                    val isKeyAvailable = ApiKeyChecker.isApiKeyAvailableForLlm(value)
                    icon = if (isKeyAvailable) {
                        UIManager.getIcon("OptionPane.informationIcon")
                    } else {
                        UIManager.getIcon("OptionPane.errorIcon")
                    }
                    toolTipText = if (isKeyAvailable) {
                        "API key found for $value"
                    } else {
                        "API key not found for $value"
                    }
                } else {
                    icon = null
                    toolTipText = null
                }
            }
            return component
        }
    }
}