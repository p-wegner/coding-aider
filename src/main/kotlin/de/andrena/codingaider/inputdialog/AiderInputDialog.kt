package de.andrena.codingaider.inputdialog

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import de.andrena.codingaider.actions.SettingsAction
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.history.AiderHistoryHandler
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.ApiKeyChecker
import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.plaf.basic.BasicSplitPaneDivider
import javax.swing.plaf.basic.BasicSplitPaneUI

class AiderInputDialog(
    val project: Project,
    files: List<FileData>,
    initialText: String = ""
) : DialogWrapper(project) {
    private val settings = AiderSettings.getInstance(project)
    private val inputTextArea = JTextArea(5, 50).apply {
        text = initialText
    }
    private val yesCheckBox = JCheckBox("Add --yes flag", settings.useYesFlag).apply {
        toolTipText = "Automatically answer 'yes' to prompts"
    }

    private fun addAiderDocsToPersistentFiles() {
        val fileChooser = JFileChooser().apply {
            currentDirectory = File(project.basePath)
            fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
            isMultiSelectionEnabled = true
        }
        
        if (fileChooser.showOpenDialog(contentPane) == JFileChooser.APPROVE_OPTION) {
            val selectedFiles = fileChooser.selectedFiles
            val fileDataList = selectedFiles.flatMap { file ->
                if (file.isDirectory) {
                    file.walkTopDown()
                        .filter { it.isFile }
                        .map { FileData(it.absolutePath, false) }
                        .toList()
                } else {
                    listOf(FileData(file.absolutePath, false))
                }
            }
            persistentFileManager.addAllFiles(fileDataList)
            
            // Update persistent files in AiderContextView
            aiderContextView.updatePersistentFiles(persistentFileManager.getPersistentFiles())
            
            // Refresh the context view
            aiderContextView.updateTree()
        }
    }

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
    private val modeToggle = JCheckBox("Shell Mode", settings.isShellMode).apply {
        toolTipText = "Toggle between normal mode and shell mode"
    }
    private val messageLabel = JLabel("Enter your message:")
    private val historyComboBox = ComboBox<HistoryItem>()
    private val historyHandler = AiderHistoryHandler(project.basePath ?: "")
    private val aiderContextView: AiderContextView
    private val persistentFileManager: PersistentFileManager
    private var splitPane: JSplitPane
    private val settingsButton: ActionButton

    init {
        title = "Aider Command"
        persistentFileManager = PersistentFileManager(project.basePath ?: "")
        aiderContextView = AiderContextView(project, files + persistentFileManager.getPersistentFiles()) { fileName ->
            insertTextAtCursor(fileName)
        }
        splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
        settingsButton = createSettingsButton()
        init()
        loadHistory()
        setOKButtonText("OK")
        setCancelButtonText("Cancel")
        setupKeyBindings()
        llmComboBox.selectedItem = settings.llm
        llmComboBox.renderer = LlmComboBoxRenderer()
        customizeSplitPane()

        // Set minimum size for the dialog and its components
        inputTextArea.minimumSize = Dimension(300, 100)
        aiderContextView.minimumSize = Dimension(300, 200)
    }

    private fun createSettingsButton(): ActionButton {
        val settingsAction = SettingsAction()
        val presentation = Presentation("Open Settings").apply {
            icon = AllIcons.General.Settings
            description = "Open Aider Settings"
        }
        return ActionButton(
            settingsAction,
            presentation,
            "AiderSettingsButton",
            ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
        )
    }

    private fun customizeSplitPane() {
        splitPane.setUI(object : BasicSplitPaneUI() {
            override fun createDefaultDivider(): BasicSplitPaneDivider {
                return object : BasicSplitPaneDivider(this) {
                    override fun paint(g: Graphics) {
                        g.color = UIManager.getColor("SplitPane.background")
                        g.fillRect(0, 0, width, height)
                        super.paint(g)
                    }
                }
            }
        })
        splitPane.border = null
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
        historyHandler.getInputHistory().forEach { (dateTime, command) ->
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
        }

        // First row: Shell Mode toggle, LLM selection, and History
        val firstRowPanel = JPanel(GridBagLayout())
        modeToggle.mnemonic = KeyEvent.VK_M
        firstRowPanel.add(modeToggle, GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            weightx = 0.0
            insets = JBUI.insets(0, 0, 0, 10)
        })
        val selectLlmLabel = JLabel("LLM:").apply {
            displayedMnemonic = KeyEvent.VK_L
            labelFor = llmComboBox
            toolTipText = "Select the Language Model to use"
        }
        firstRowPanel.add(selectLlmLabel, GridBagConstraints().apply {
            gridx = 1
            gridy = 0
            weightx = 0.0
            insets = JBUI.insets(0, 5, 0, 5)
        })
        firstRowPanel.add(llmComboBox, GridBagConstraints().apply {
            gridx = 2
            gridy = 0
            weightx = 0.2
            fill = GridBagConstraints.HORIZONTAL
        })
        val historyLabel = JLabel("History:").apply {
            displayedMnemonic = KeyEvent.VK_H
            labelFor = historyComboBox
            toolTipText = "Select from previous commands"
        }
        firstRowPanel.add(historyLabel, GridBagConstraints().apply {
            gridx = 3
            gridy = 0
            weightx = 0.0
            insets = JBUI.insets(0, 15, 0, 5)
        })
        historyComboBox.preferredSize = Dimension(200, historyComboBox.preferredSize.height)
        firstRowPanel.add(historyComboBox, GridBagConstraints().apply {
            gridx = 4
            gridy = 0
            weightx = 0.7
            fill = GridBagConstraints.HORIZONTAL
        })
        firstRowPanel.add(settingsButton, GridBagConstraints().apply {
            gridx = 5
            gridy = 0
            weightx = 0.0
            fill = GridBagConstraints.NONE
            insets = JBUI.insets(0, 10, 0, 0)
        })
        topPanel.add(firstRowPanel, gbc)

        // Second row: Message label and input area
        gbc.gridy++
        messageLabel.displayedMnemonic = KeyEvent.VK_E
        messageLabel.labelFor = inputTextArea
        topPanel.add(messageLabel, gbc)
        gbc.gridy++
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        topPanel.add(JBScrollPane(inputTextArea), gbc)

        // Third row: Yes flag and additional arguments
        gbc.gridy++
        gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        val flagAndArgsPanel = JPanel(GridBagLayout())
        yesCheckBox.mnemonic = KeyEvent.VK_Y
        flagAndArgsPanel.add(yesCheckBox, GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            weightx = 0.0
            insets = JBUI.insets(0, 0, 0, 10)
        })
        val additionalArgsLabel = JLabel("Args:").apply {
            displayedMnemonic = KeyEvent.VK_A
            labelFor = additionalArgsField
            toolTipText = "Additional arguments for the Aider command"
        }
        flagAndArgsPanel.add(additionalArgsLabel, GridBagConstraints().apply {
            gridx = 1
            gridy = 0
            weightx = 0.0
            insets = JBUI.insets(0, 5, 0, 5)
        })
        flagAndArgsPanel.add(additionalArgsField, GridBagConstraints().apply {
            gridx = 2
            gridy = 0
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
        })
        topPanel.add(flagAndArgsPanel, gbc)

        // Context view
        val fileActionGroup = DefaultActionGroup().apply {
            add(object : AnAction("Add Files", "Add files to persistent files", AllIcons.Actions.MenuOpen) {
                override fun actionPerformed(e: AnActionEvent) {
                    addAiderDocsToPersistentFiles()
                }
                override fun getActionUpdateThread()= ActionUpdateThread.BGT
                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = true
                    e.presentation.text = "Add Files (Ctrl+A)"
                }
            }.also { it.registerCustomShortcutSet(CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK)), aiderContextView) })
            
            add(object : AnAction(
                "Remove Files",
                "Remove selected files from the context view",
                AllIcons.Actions.Cancel
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    aiderContextView.removeSelectedFiles()
                }
                override fun getActionUpdateThread()= ActionUpdateThread.BGT
                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = true
                    e.presentation.text = "Remove Files (Del)"
                }
            })
        }

        val fileStatusActionGroup = DefaultActionGroup().apply {
            add(object : AnAction(
                "Toggle Read-Only Mode",
                "Toggle Read-Only Mode for selected file",
                AllIcons.Actions.Edit
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    aiderContextView.toggleReadOnlyMode()
                }

                override fun getActionUpdateThread()= ActionUpdateThread.BGT
                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = true
                    e.presentation.text = "Toggle Read-Only (Ctrl+R)"
                }
            }.also { it.registerCustomShortcutSet(CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK)), aiderContextView) })
            
            add(object : AnAction(
                "Toggle Persistent Files",
                "Toggle selected files' persistent status",
                AllIcons.Actions.MenuSaveall
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    aiderContextView.togglePersistentFile()
                }
                override fun getActionUpdateThread()= ActionUpdateThread.BGT
                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = true
                    e.presentation.text = "Toggle Persistent (Ctrl+P)"
                }
            }.also { it.registerCustomShortcutSet(CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK)), aiderContextView) })
        }

        val combinedActionGroup = DefaultActionGroup().apply {
            addAll(fileActionGroup)
            addSeparator()
            addAll(fileStatusActionGroup)
        }

        val toolbar = ActionManager.getInstance().createActionToolbar("AiderContextToolbar", combinedActionGroup, true)
        toolbar.targetComponent = aiderContextView
        val contextPanel = JPanel(BorderLayout()).apply {
            add(toolbar.component, BorderLayout.NORTH)
            add(aiderContextView, BorderLayout.CENTER)
        }

        splitPane.topComponent = topPanel
        splitPane.bottomComponent = contextPanel
        splitPane.resizeWeight = 0.6
        panel.add(splitPane, BorderLayout.CENTER)

        modeToggle.addActionListener {
            val isShellMode = modeToggle.isSelected
            inputTextArea.isVisible = !isShellMode
            messageLabel.isVisible = !isShellMode
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

    private fun insertTextAtCursor(text: String) {
        val caretPosition = inputTextArea.caretPosition
        inputTextArea.insert(text, caretPosition)
    }

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
                if (apiKey != null && !ApiKeyChecker.isApiKeyAvailableForLlm(value)) {
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

}
