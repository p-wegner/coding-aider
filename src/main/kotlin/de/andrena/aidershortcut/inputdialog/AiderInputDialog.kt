package de.andrena.aidershortcut.inputdialog

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import de.andrena.aidershortcut.command.FileData
import de.andrena.aidershortcut.commandhistory.AiderHistoryHandler
import java.awt.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.*

class AiderInputDialog(private val project: Project, files: List<FileData>) : DialogWrapper(project) {
    private val inputTextArea = JTextArea(5, 30)
    private val yesCheckBox = JCheckBox("Add --yes flag", false)
    private val commandOptions = arrayOf("--mini", "--sonnet", "--4o", "--deepseek")
    private val commandComboBox = JComboBox(commandOptions)
    private val additionalArgsField = JTextField(20)
    private val modeToggle = JCheckBox("Shell Mode", false)
    private val messageLabel = JLabel("Enter your message:")
    private val historyComboBox = JComboBox<HistoryItem>()
    private val historyHandler = AiderHistoryHandler(project.basePath ?: "")
    private val aiderContextView: AiderContextView

    init {
        title = "Aider Command"
        aiderContextView = AiderContextView(project, files)
        init()
        loadHistory()
    }

    private fun loadHistory() {
        historyHandler.getHistory().forEach { (dateTime, command) ->
            historyComboBox.addItem(HistoryItem(command, dateTime))
        }
        historyComboBox.renderer = HistoryItemRenderer()
        historyComboBox.addItem(HistoryItem("Select previous command...", null))
        historyComboBox.addActionListener {
            if (historyComboBox.selectedIndex >= 0 && historyComboBox.selectedItem is HistoryItem) {
                val selectedItem = historyComboBox.selectedItem as HistoryItem
                if (selectedItem.dateTime != null) {
                    inputTextArea.text = selectedItem.command
                }
            }
        }
    }

    private data class HistoryItem(val command: String, val dateTime: LocalDateTime?)

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
                text = value.command
                if (value.dateTime != null) {
                    toolTipText = value.dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                } else {
                    toolTipText = null
                }
            }
            return component
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(5, 5, 5, 5)
            weightx = 1.0
            weighty = 0.0
            gridx = 0
            gridy = 0
            gridwidth = GridBagConstraints.REMAINDER
        }

        panel.add(modeToggle, gbc)
        gbc.gridy++
        panel.add(historyComboBox, gbc)
        gbc.gridy++
        panel.add(messageLabel, gbc)
        gbc.gridy++
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        panel.add(JBScrollPane(inputTextArea), gbc)
        gbc.gridy++
        gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(yesCheckBox, gbc)
        gbc.gridy++
        gbc.gridwidth = 1
        panel.add(JLabel("Select command:"), gbc)
        gbc.gridx = 1
        gbc.gridwidth = GridBagConstraints.REMAINDER
        panel.add(commandComboBox, gbc)
        gbc.gridy++
        gbc.gridx = 0
        gbc.gridwidth = 1
        panel.add(JLabel("Additional arguments:"), gbc)
        gbc.gridx = 1
        gbc.gridwidth = GridBagConstraints.REMAINDER
        panel.add(additionalArgsField, gbc)
        gbc.gridy++
        gbc.gridx = 0
        gbc.gridwidth = GridBagConstraints.REMAINDER
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH

        val actionGroup = DefaultActionGroup().apply {
            add(object : com.intellij.openapi.actionSystem.AnAction(
                "Toggle Read-Only Mode",
                "Toggle Read-Only Mode for selected file",
                AllIcons.Actions.Edit
            ) {
                override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
                    aiderContextView.toggleReadOnlyMode()
                }
            })
            add(object : com.intellij.openapi.actionSystem.AnAction(
                "Toggle Persistent Files",
                "Toggle selected files' persistent status",
                AllIcons.Actions.MenuSaveall
            ) {
                override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
                    aiderContextView.togglePersistentFile()
                }
            })
        }

        val toolbar = ActionManager.getInstance().createActionToolbar("AiderContextToolbar", actionGroup, true)
        val contextPanel = JPanel(BorderLayout()).apply {
            add(toolbar.component, BorderLayout.NORTH)
            add(aiderContextView, BorderLayout.CENTER)
        }

        panel.add(contextPanel, gbc)

        modeToggle.addActionListener {
            val isShellMode = modeToggle.isSelected
            inputTextArea.isVisible = !isShellMode
            messageLabel.text = if (isShellMode) "Shell mode enabled" else "Enter your message:"
        }

        return panel
    }

    override fun doOKAction() {
        super.doOKAction()
    }

    override fun doCancelAction() {
        super.doCancelAction()
    }

    fun getInputText(): String = inputTextArea.text
    fun isYesFlagChecked(): Boolean = yesCheckBox.isSelected
    fun getSelectedCommand(): String = commandComboBox.selectedItem as String
    fun getAdditionalArgs(): String = additionalArgsField.text
    fun getAllFiles(): List<FileData> = aiderContextView.getAllFiles()
    fun isShellMode(): Boolean = modeToggle.isSelected

}
