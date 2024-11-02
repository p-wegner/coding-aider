package de.andrena.codingaider.inputdialog

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.addKeyboardAction
import com.intellij.ui.LayeredIcon
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.util.textCompletion.TextCompletionUtil
import com.intellij.util.ui.JBUI
import de.andrena.codingaider.actions.ide.SettingsAction
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.services.AiderDialogStateService
import de.andrena.codingaider.services.AiderHistoryService
import de.andrena.codingaider.services.PersistentFileService
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import de.andrena.codingaider.services.TokenCountService
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.DefaultApiKeyChecker
import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.*

class AiderInputDialog(
    val project: Project,
    files: List<FileData>,
    initialText: String = "",
    private val apiKeyChecker: ApiKeyChecker = DefaultApiKeyChecker()
) : DialogWrapper(project) {
    private val tokenCountService = project.getService(TokenCountService::class.java)
    private val settings = getInstance()
    private val aiderCompletionProvider = AiderCompletionProvider(project, files)


    private val inputTextField: TextFieldWithAutoCompletion<String> =
        EditorFactory.getInstance().createDocument(initialText)
            .let { _ ->
                TextFieldWithAutoCompletion(project, aiderCompletionProvider, false, initialText)
            }
            .apply {
                setOneLineMode(false)
                addSettingsProvider { editor ->
                    editor?.apply {
                        setHorizontalScrollbarVisible(true)
                        setVerticalScrollbarVisible(true)
                        settings.apply {
                            isShowIntentionBulb = true
                            isLineNumbersShown = true
                            isAutoCodeFoldingEnabled = true
                        }
                    }
                }
                this.getEditor(true)?.let { editor ->
                    TextCompletionUtil.installCompletionHint(editor)
                }
                val value : DocumentListener = object : DocumentListener {
                    override fun documentChanged(e: DocumentEvent) = updateTokenCount()
                }
                document.addDocumentListener(value)
            }
    private val yesCheckBox = JCheckBox("Add --yes flag", settings.useYesFlag).apply {
        toolTipText = "Automatically answer 'yes' to prompts"
    }

    private fun updateTokenCount() {
        val messageTokens = tokenCountService.countTokensInText(getInputText())
        val fileTokens = tokenCountService.countTokensInFiles(getAllFiles())
        tokenCountLabel.text = "Tokens: ${messageTokens + fileTokens}"
    }

    private fun addFilesToContext() {
        val fileChooser = JFileChooser().apply {
            currentDirectory = File(project.basePath!!)
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
            aiderContextView.addFilesToContext(fileDataList)
        }
    }

    private fun addOpenFilesToContext() = aiderContextView.addOpenFilesToContext()

    private val llmOptions = apiKeyChecker.getAllLlmOptions().toTypedArray()
    private val llmComboBox = object : ComboBox<String>(llmOptions) {
        override fun getToolTipText(): String? {
            val selectedItem = selectedItem as? String ?: return null
            return if (apiKeyChecker.isApiKeyAvailableForLlm(selectedItem)) {
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
    private val structuredModeCheckBox = JCheckBox("Structured Mode", settings.useStructuredMode).apply {
        toolTipText = "<html>Enable structured mode for organized feature development:<br>" +
                "1. Describe a feature to generate a plan and checklist<br>" +
                "2. Plans are stored in .coding-aider-plans directory<br>" +
                "3. Aider updates plans based on progress and new requirements<br>" +
                "4. Implements plan step-by-step when in context<br>" +
                "5. Message can be left empty to continue with an existing plan<br>" +
                "Use for better tracking and systematic development</html>"
    }
    private val messageLabel = JLabel("Enter your message:")
    private val tokenCountLabel = JLabel("Tokens: 0")
    private val historyComboBox = ComboBox<HistoryItem>()
    private val historyService = AiderHistoryService.getInstance(project)
    private val aiderContextView: AiderContextView
    private val persistentFileService: PersistentFileService
    private var splitPane: OnePixelSplitter
    private val settingsButton: ActionButton

    init {
        title = "Aider Command"
        persistentFileService = PersistentFileService.getInstance(project)
        aiderContextView = AiderContextView(
            project,
            files + persistentFileService.getPersistentFiles(),
            { fileName -> insertTextAtCursor(fileName) },
            { updateTokenCount() }
        )
        splitPane = OnePixelSplitter(true, 0.6f)
        settingsButton = createSettingsButton()
        init()
        loadHistory()
        setOKButtonText("OK")
        setCancelButtonText("Cancel")
        setupKeyBindings()
        llmComboBox.selectedItem = settings.llm
        llmComboBox.renderer = LlmComboBoxRenderer()

        // Set minimum size for the dialog and its components
        inputTextField.minimumSize = Dimension(300, 100)
        aiderContextView.minimumSize = Dimension(300, 200)
    }

    private fun createSettingsButton(): ActionButton {
        val settingsAction = SettingsAction()
        val presentation = Presentation("Open Settings").apply {
            icon = AllIcons.General.Settings
            description = "Open aider settings"
        }
        return ActionButton(
            settingsAction,
            presentation,
            "AiderSettingsButton",
            ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
        )
    }

    private fun setupKeyBindings() {
        inputTextField.addKeyboardAction(
            listOf(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.ALT_DOWN_MASK))
        ) { navigateHistory(-1) }
        inputTextField.addKeyboardAction(
            listOf(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.ALT_DOWN_MASK))
        ) { navigateHistory(1) }
    }

    private fun navigateHistory(direction: Int) {
        val currentIndex = historyComboBox.selectedIndex
        val newIndex = (currentIndex + direction).coerceIn(0, historyComboBox.itemCount - 1)
        if (newIndex != currentIndex) {
            historyComboBox.selectedIndex = newIndex
            val selectedItem = historyComboBox.selectedItem as? HistoryItem
            inputTextField.text = selectedItem?.command?.joinToString("\n") ?: ""
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
            inputTextField.requestFocus()
            inputTextField.editor?.caretModel?.moveToOffset(inputTextField.text.length)
        }
    }

    private fun loadHistory() {
        historyComboBox.addItem(HistoryItem(emptyList(), null))  // Empty entry
        historyService.getInputHistory().forEach { (dateTime, command) ->
            historyComboBox.addItem(HistoryItem(command, dateTime))
        }
        historyComboBox.renderer = HistoryItemRenderer()
        historyComboBox.selectedIndex = 0  // Select the empty entry initially
        historyComboBox.addActionListener {
            if (historyComboBox.selectedIndex > 0 && historyComboBox.selectedItem is HistoryItem) {
                val selectedItem = historyComboBox.selectedItem as HistoryItem
                inputTextField.text = selectedItem.command.joinToString("\n")
            } else {
                inputTextField.text = ""  // Clear the input area when empty entry is selected
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

        // First row: Shell Mode toggle, Structured Mode toggle, LLM selection, History, and Token Count
        val firstRowPanel = JPanel(GridBagLayout())
        modeToggle.mnemonic = KeyEvent.VK_M
        firstRowPanel.add(modeToggle, GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            weightx = 0.0
            insets = JBUI.insetsRight(10)
        })
        structuredModeCheckBox.mnemonic = KeyEvent.VK_S
        firstRowPanel.add(structuredModeCheckBox, GridBagConstraints().apply {
            gridx = 1
            gridy = 0
            weightx = 0.0
            insets = JBUI.insetsRight(10)
        })
        val selectLlmLabel = JLabel("LLM:").apply {
            displayedMnemonic = KeyEvent.VK_L
            labelFor = llmComboBox
            toolTipText = "Select the Language Model to use"
        }
        firstRowPanel.add(selectLlmLabel, GridBagConstraints().apply {
            gridx = 2
            gridy = 0
            weightx = 0.0
            insets = JBUI.insets(0, 5)
        })
        firstRowPanel.add(llmComboBox, GridBagConstraints().apply {
            gridx = 3
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
            gridx = 4
            gridy = 0
            weightx = 0.0
            insets = JBUI.insets(0, 15, 0, 5)
        })
        historyComboBox.preferredSize = Dimension(200, historyComboBox.preferredSize.height)
        firstRowPanel.add(historyComboBox, GridBagConstraints().apply {
            gridx = 5
            gridy = 0
            weightx = 0.7
            fill = GridBagConstraints.HORIZONTAL
        })
        val restoreButton = ActionButton(
            object : AnAction() {
                override fun actionPerformed(e: AnActionEvent) {
                    restoreLastState()
                }
            },
            Presentation("Restore To Last Executed Command").apply {
                icon = AllIcons.Actions.Rollback
                description = "Restore dialog to last used state"
            },
            "AiderRestoreButton",
            ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
        )

        firstRowPanel.add(restoreButton, GridBagConstraints().apply {
            gridx = 6
            gridy = 0
            weightx = 0.0
            fill = GridBagConstraints.NONE
            insets = JBUI.insetsLeft(10)
        })

        firstRowPanel.add(settingsButton, GridBagConstraints().apply {
            gridx = 7
            gridy = 0
            weightx = 0.0
            fill = GridBagConstraints.NONE
            insets = JBUI.insetsLeft(10)
        })
        firstRowPanel.add(tokenCountLabel, GridBagConstraints().apply {
            gridx = 8
            gridy = 0
            weightx = 0.0
            insets = JBUI.insetsLeft(10)
        })
        topPanel.add(firstRowPanel, gbc)

        // Update token count initially
        updateTokenCount()

        // Second row: Message label and input area
        gbc.gridy++
        messageLabel.displayedMnemonic = KeyEvent.VK_E
        messageLabel.labelFor = inputTextField
        topPanel.add(messageLabel, gbc)
        gbc.gridy++
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        topPanel.add(inputTextField, gbc)

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
            insets = JBUI.insetsRight(10)
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
            insets = JBUI.insets(0, 5)
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
            add(object : AnAction("Add Files", "Add files to persistent files", LayeredIcon.ADD_WITH_DROPDOWN) {
                override fun actionPerformed(e: AnActionEvent) {
                    val popup = JPopupMenu()
                    popup.add(JMenuItem("From Project").apply {
                        addActionListener {
                            addFilesToContext()
                        }
                    })
                    popup.add(JMenuItem("Add Open Files").apply {
                        addActionListener {
                            addOpenFilesToContext()
                        }
                    })
                    val component = e.inputEvent?.component
                    // TODO place the popup next to the Action Toolbar if shortcut is used
                    popup.show(component, 0, component?.height ?: 0)
                }

                override fun getActionUpdateThread() = ActionUpdateThread.BGT
                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = true
                    e.presentation.text = "Add Files"
                }
            }.also {
                it.registerCustomShortcutSet(
                    CustomShortcutSet(
                        KeyStroke.getKeyStroke(
                            KeyEvent.VK_F,
                            InputEvent.ALT_DOWN_MASK
                        )
                    ), aiderContextView
                )
            })

            add(object : AnAction(
                "Remove Files",
                "Remove selected files from the context view",
                AllIcons.Actions.Cancel
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    aiderContextView.removeSelectedFiles()
                }

                override fun getActionUpdateThread() = ActionUpdateThread.BGT
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

                override fun getActionUpdateThread() = ActionUpdateThread.BGT
                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = true
                    e.presentation.text = "Toggle Read-Only"
                }
            }.also {
                it.registerCustomShortcutSet(
                    CustomShortcutSet(
                        KeyStroke.getKeyStroke(
                            KeyEvent.VK_R,
                            InputEvent.CTRL_DOWN_MASK
                        )
                    ), aiderContextView
                )
            })

            add(object : AnAction(
                "Toggle Persistent Files",
                "Toggle selected files' persistent status",
                AllIcons.Actions.MenuSaveall
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    aiderContextView.togglePersistentFile()
                }

                override fun getActionUpdateThread() = ActionUpdateThread.BGT
                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = true
                    e.presentation.text = "Toggle Persistent"
                }
            }.also {
                it.registerCustomShortcutSet(
                    CustomShortcutSet(
                        KeyStroke.getKeyStroke(
                            KeyEvent.VK_P,
                            InputEvent.CTRL_DOWN_MASK
                        )
                    ), aiderContextView
                )
            })
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

        splitPane.firstComponent = topPanel
        splitPane.secondComponent = contextPanel
        panel.add(splitPane, BorderLayout.CENTER)

        modeToggle.addActionListener {
            val isShellMode = modeToggle.isSelected
            inputTextField.isVisible = !isShellMode
            messageLabel.isVisible = !isShellMode
            messageLabel.text = if (isShellMode) "Shell mode enabled" else "Enter your message:"
        }

        // Set focus on the input text area when the dialog is opened
        inputTextField.requestFocus()

        return panel
    }

    fun getInputText(): String = inputTextField.text
    fun isYesFlagChecked(): Boolean = yesCheckBox.isSelected
    fun getLlm(): String = llmComboBox.selectedItem as String
    fun getAdditionalArgs(): String = additionalArgsField.text
    fun getAllFiles(): List<FileData> = aiderContextView?.getAllFiles() ?: emptyList()
    fun isShellMode(): Boolean = modeToggle.isSelected
    fun isStructuredMode(): Boolean = structuredModeCheckBox.isSelected

    private fun restoreLastState() {
        AiderDialogStateService.getInstance(project).getLastState()?.let { state ->
            inputTextField.text = state.message
            yesCheckBox.isSelected = state.useYesFlag
            llmComboBox.selectedItem = state.llm
            additionalArgsField.text = state.additionalArgs
            modeToggle.isSelected = state.isShellMode
            structuredModeCheckBox.isSelected = state.isStructuredMode
            aiderContextView.setFiles(state.files)
        }
    }

    private fun insertTextAtCursor(text: String) {
        WriteCommandAction.runWriteCommandAction(project) {
            inputTextField.editor?.document?.insertString(inputTextField.editor?.caretModel?.offset ?: 0, text)
        }
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
                val apiKey = apiKeyChecker.getApiKeyForLlm(value)
                if (apiKey != null && !apiKeyChecker.isApiKeyAvailableForLlm(value)) {
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

