package de.andrena.codingaider.inputdialog

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.addKeyboardAction
import com.intellij.ui.LayeredIcon
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.SegmentedButton
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.textCompletion.TextCompletionUtil
import com.intellij.util.ui.JBUI
import de.andrena.codingaider.actions.ide.SettingsAction
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.services.*
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import de.andrena.codingaider.settings.AiderProjectSettings
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.DefaultApiKeyChecker
import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.io.File
import org.pushingpixels.trident.Timeline
import org.pushingpixels.trident.callback.TimelineCallback
import org.pushingpixels.trident.ease.Spline
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
        EditorFactory.getInstance().createDocument(initialText).let { _ ->
            TextFieldWithAutoCompletion(project, aiderCompletionProvider, false, initialText)
        }.apply {
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
            val value: DocumentListener = object : DocumentListener {
                override fun documentChanged(e: DocumentEvent) = updateTokenCount()
            }
            document.addDocumentListener(value)
        }
    private val yesCheckBox = JCheckBox("Add --yes flag", settings.useYesFlag).apply {
        toolTipText = "Automatically answer 'yes' to prompts"
    }

    private fun updateTokenCount() {
        val messageTokens = tokenCountService.countTokensInText(getInputText())
        tokenCountLabel.text = "Tokens: ${messageTokens + allFileTokens}"
    }

    val lazyCacheDelegate = LazyCacheDelegate { tokenCountService.countTokensInFiles(getAllFiles()) }
    val allFileTokens by lazyCacheDelegate

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
                    file.walkTopDown().filter { it.isFile }.map { FileData(it.absolutePath, false) }.toList()
                } else {
                    listOf(FileData(file.absolutePath, false))
                }
            }
            aiderContextView.addFilesToContext(fileDataList)
        }
    }

    private fun addOpenFilesToContext() = aiderContextView.addOpenFilesToContext()

    private val llmOptions = apiKeyChecker.getAllLlmOptions().toTypedArray()

    private val projectSettings = AiderProjectSettings.getInstance(project)
    private val llmComboBox = object : ComboBox<String>(llmOptions) {
        override fun getToolTipText(): String? {
            return null
            // TODO: Enable this tooltip when slow thread error is fixed
//            val selectedItem = selectedItem as? String ?: return null
//            return if (apiKeyChecker.isApiKeyAvailableForLlm(selectedItem)) {
//                "API key found for $selectedItem"
//            } else {
//                "API key not found for $selectedItem"
//            }
        }
    }
    private val additionalArgsField = JTextField(settings.additionalArgs, 20)
    private var initialMode = if (settings.isShellMode) AiderMode.SHELL
    else if (settings.useStructuredMode) AiderMode.STRUCTURED
    else AiderMode.NORMAL

    private var modeSegmentedButton: SegmentedButton<AiderMode>? = null
    private val modeSegmentedButtonPanel: DialogPanel
    private val messageLabel: JLabel
    private val tokenCountLabel = JLabel("Tokens: 0").apply {
        toolTipText =
            "The actual token count may vary depending on the model. The displayed number uses GPT-4O encoding as a heuristic."
    }
    private val historyComboBox = ComboBox<HistoryItem>()
    private val historyService = project.service<AiderHistoryService>()
    private val aiderContextView: AiderContextView
    private val persistentFileService: PersistentFileService
    private var splitPane: OnePixelSplitter
    private val settingsButton: ActionButton
    private lateinit var collapseButton: ActionButton

    init {
        title = "Aider Command"
        messageLabel = JLabel("Enter your message:")

        persistentFileService = project.service<PersistentFileService>()
        aiderContextView = AiderContextView(project,
            files + persistentFileService.getPersistentFiles(),
            { fileName -> insertTextAtCursor(fileName) },
            {
                lazyCacheDelegate.evict()
                updateTokenCount()
                updateModeUI()
            }

        )
        splitPane = OnePixelSplitter(true, 0.6f)
        settingsButton = createSettingsButton()
        lazyCacheDelegate.evict()
        modeSegmentedButtonPanel = panel {
            row {
                cell(JBLabel("Mode:"))
                modeSegmentedButton = segmentedButton(AiderMode.values().map { it }) { selectedItem ->
                    text = selectedItem.displayName
                    toolTipText = selectedItem.tooltip
                    icon = selectedItem.icon
                }.apply {
                    this.selectedItem = initialMode
                    whenItemSelected { updateModeUI() }
                }
            }
        }
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
        modeSegmentedButton?.selectedItem = initialMode
        updateModeUI()
    }

    private fun createSettingsButton(): ActionButton {
        val settingsAction = SettingsAction()
        val presentation = Presentation("Open Settings").apply {
            icon = AllIcons.General.Settings
            description = "Open aider settings"
        }
        return ActionButton(
            settingsAction, presentation, "AiderSettingsButton", ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
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
            list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
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
        firstRowPanel.add(modeSegmentedButtonPanel, GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            weightx = 0.0
            gridwidth = 2
            insets = JBUI.insetsRight(10)
        })
        val selectLlmLabel = JLabel("LLM:").apply {
            displayedMnemonic = KeyEvent.VK_L
            labelFor = llmComboBox
            toolTipText = "Select the Language Model to use"
        }
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
        val restoreButton = ActionButton(object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                restoreLastState()
            }
        }, Presentation("Restore To Last Executed Command").apply {
            icon = AllIcons.Actions.Rollback
            description = "Restore dialog to last used state"
        }, "AiderRestoreButton", ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
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

        // Options panel with collapsible UI
        gbc.gridy++
        gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        
        val optionsPanel = com.intellij.ui.components.panels.Wrapper()
        val flagAndArgsPanel = createOptionsPanel()
        
        // Initialize panel with content if not collapsed
        if (!projectSettings.isOptionsCollapsed) {
            optionsPanel.setContent(flagAndArgsPanel)
        }
        
        val collapseButton = ActionButton(
            object : AnAction() {
                override fun actionPerformed(e: AnActionEvent) {
                    projectSettings.isOptionsCollapsed = !projectSettings.isOptionsCollapsed
                    updateOptionsPanel(optionsPanel, flagAndArgsPanel, collapseButton)
                }
            },
            Presentation().apply {
                icon = if (projectSettings.isOptionsCollapsed) AllIcons.General.ArrowRight else AllIcons.General.ArrowDown
                text = "Toggle Options"
                description = "Show/hide additional options (LLM selection, flags and arguments)"
            },
            "AiderOptionsButton",
            ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
        )
        
        val optionsHeader = JPanel(BorderLayout()).apply {
            add(collapseButton, BorderLayout.WEST)
            add(JLabel("Additional Options"), BorderLayout.CENTER)
            border = JBUI.Borders.empty(2)
        }
        
        topPanel.add(optionsHeader, gbc.apply { gridy++ })
        topPanel.add(optionsPanel, gbc.apply { gridy++ })
        
        updateOptionsPanel(optionsPanel, flagAndArgsPanel, collapseButton)

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
                            KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK
                        )
                    ), aiderContextView
                )
            })

            add(object : AnAction(
                "Remove Files", "Remove selected files from the context view", AllIcons.Actions.Cancel
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
                "Toggle Read-Only Mode", "Toggle Read-Only Mode for selected file", AllIcons.Actions.Edit
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
                            KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK
                        )
                    ), aiderContextView
                )
            })

            add(object : AnAction(
                "Toggle Persistent Files", "Toggle selected files' persistent status", AllIcons.Actions.MenuSaveall
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
                            KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK
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


        return panel
    }

    fun getInputText(): String = inputTextField.text

    fun isYesFlagChecked(): Boolean = yesCheckBox.isSelected
    fun getLlm(): String = llmComboBox.selectedItem as String
    fun getAdditionalArgs(): String = additionalArgsField.text
    fun getAllFiles(): List<FileData> = aiderContextView?.getAllFiles() ?: emptyList()
    val selectedMode get() = modeSegmentedButton?.selectedItem ?: initialMode
    fun isShellMode(): Boolean = selectedMode == AiderMode.SHELL
    fun isStructuredMode(): Boolean = selectedMode == AiderMode.STRUCTURED

    private fun updateModeUI() {
        inputTextField.isEnabled = selectedMode != AiderMode.SHELL
        messageLabel.text = when (selectedMode) {
            AiderMode.SHELL -> "Shell mode enabled, no message required"
            AiderMode.STRUCTURED -> getStructuredModeMessageLabel()
            else -> "Enter your message:"
        }
    }

    private fun getStructuredModeMessageLabel(): String {
        val existingPlans =
            project.service<AiderPlanService>().getExistingPlans(persistentFileService.getPersistentFiles())
        if (existingPlans.isNotEmpty()) {
            val firstPlan = existingPlans.first()
            val planName = firstPlan.filePath.substringAfterLast("/")
            return "Continue with the plan $planName (message may be left empty):"
        }
        return "Enter feature description that will be used to create a plan:"
    }

    /**
     * Creates the collapsible options panel containing LLM selection, yes flag and additional arguments.
     * The panel's collapsed state is persisted in project settings.
     */
    private fun createOptionsPanel(): JPanel {
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(0, 10, 10, 10)
            
            val contentPanel = JPanel(GridBagLayout()).apply {
                border = JBUI.Borders.empty(5)
                background = UIManager.getColor("Panel.background").brighter()
                
                val gbc = GridBagConstraints()
                
                // LLM selection
                val selectLlmLabel = JLabel("LLM:").apply {
                    displayedMnemonic = KeyEvent.VK_L
                    labelFor = llmComboBox
                    toolTipText = "Select the Language Model to use"
                }
                add(selectLlmLabel, GridBagConstraints().apply {
                    gridx = 0
                    gridy = 0
                    weightx = 0.0
                    insets = JBUI.insets(5)
                })
                add(llmComboBox, GridBagConstraints().apply {
                    gridx = 1
                    gridy = 0
                    weightx = 1.0
                    fill = GridBagConstraints.HORIZONTAL
                    insets = JBUI.insets(5)
                })
                
                // Yes flag
                yesCheckBox.mnemonic = KeyEvent.VK_Y
                add(yesCheckBox, GridBagConstraints().apply {
                    gridx = 0
                    gridy = 1
                    weightx = 0.0
                    gridwidth = 2
                    insets = JBUI.insets(5)
                })
                
                // Additional args
                val additionalArgsLabel = JLabel("Args:").apply {
                    displayedMnemonic = KeyEvent.VK_A
                    labelFor = additionalArgsField
                    toolTipText = "Additional arguments for the Aider command"
                }
                add(additionalArgsLabel, GridBagConstraints().apply {
                    gridx = 0
                    gridy = 2
                    weightx = 0.0
                    insets = JBUI.insets(5)
                })
                add(additionalArgsField, GridBagConstraints().apply {
                    gridx = 1
                    gridy = 2
                    weightx = 1.0
                    fill = GridBagConstraints.HORIZONTAL
                    insets = JBUI.insets(5)
                })
            }
            
            add(contentPanel, BorderLayout.CENTER)
        }
    }
    
    /**
     * Updates the options panel visibility state with animation.
     * The collapsed state is persisted in project settings.
     * 
     * @param wrapper The wrapper component containing the options panel
     * @param panel The options panel to show/hide
     * @param collapseButton The button that toggles the panel state
     */
    private fun updateOptionsPanel(wrapper: com.intellij.ui.components.panels.Wrapper, panel: JPanel, collapseButton: ActionButton) {
        val isCollapsed = projectSettings.isOptionsCollapsed
        
        val animation = PanelAnimation(wrapper)
        
        if (isCollapsed) {
            animation.animate(
                startHeight = panel.preferredSize.height,
                endHeight = 0,
                onComplete = { wrapper.setContent(null) }
            )
            collapseButton.presentation.icon = AllIcons.General.ArrowRight
        } else {
            wrapper.setContent(panel)
            animation.animate(
                startHeight = 0,
                endHeight = panel.preferredSize.height
            )
            collapseButton.presentation.icon = AllIcons.General.ArrowDown
        }
    }

    private fun restoreLastState() {
        AiderDialogStateService.getInstance(project).getLastState()?.let { state ->
            inputTextField.text = state.message
            yesCheckBox.isSelected = state.useYesFlag
            llmComboBox.selectedItem = state.llm
            additionalArgsField.text = state.additionalArgs
            initialMode = if (state.isShellMode) AiderMode.SHELL
            else if (state.isStructuredMode) AiderMode.STRUCTURED
            else AiderMode.NORMAL
            modeSegmentedButton?.selectedItem = initialMode
            aiderContextView.setFiles(state.files)
        }
    }

    private fun insertTextAtCursor(text: String) {
        WriteCommandAction.runWriteCommandAction(project) {
            inputTextField.editor?.document?.insertString(inputTextField.editor?.caretModel?.offset ?: 0, text)
        }
    }

    private inner class LlmComboBoxRenderer : DefaultListCellRenderer() {
        private val apiKeyStatus = mutableMapOf<String, Boolean>()

        init {
            // Initialize status checking in background
            com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
                llmOptions.forEach { llm ->
                    apiKeyStatus[llm] = apiKeyChecker.getApiKeyForLlm(llm)?.let {
                        apiKeyChecker.isApiKeyAvailableForLlm(llm)
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

}


class LazyCacheDelegate<T>(private val initializer: () -> T) {
    private var cachedValue: T? = null
    private var isInitialized = false

    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T {
        if (!isInitialized) {
            cachedValue = initializer()
            isInitialized = true
        }
        return cachedValue!!
    }

    fun evict() {
        cachedValue = null
        isInitialized = false
    }
}

