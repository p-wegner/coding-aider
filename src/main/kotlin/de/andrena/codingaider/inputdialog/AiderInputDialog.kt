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
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.SegmentedButton
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.textCompletion.TextCompletionUtil
import com.intellij.util.ui.JBUI
import com.sun.java.accessibility.util.AWTEventMonitor.addActionListener
import de.andrena.codingaider.actions.ide.SettingsAction
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.services.*
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.settings.AiderProjectSettings
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.CollapsiblePanel
import de.andrena.codingaider.utils.DefaultApiKeyChecker
import de.andrena.codingaider.utils.PanelAnimation
import java.awt.*
import java.awt.event.KeyEvent
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
    private val historyComboBox = AiderHistoryComboBox(project, inputTextField)
    private val aiderContextView: AiderContextView
    private val persistentFileService: PersistentFileService
    private var splitPane: OnePixelSplitter
    private val settingsButton: ActionButton
    private val flagAndArgsPanel = createOptionsPanel()
    private val optionsPanel = com.intellij.ui.components.panels.Wrapper().apply {
        setContent(flagAndArgsPanel)
        isVisible = true // Always visible for animation
        preferredSize = if (!projectSettings.isOptionsCollapsed) null else Dimension(0, 0)
    }
    private val panelAnimation = PanelAnimation(optionsPanel)
    private fun createCollapseButton(
        title: String,
        isCollapsedGetter: () -> Boolean,
        panel: JComponent,
        contentPanel: JComponent,
        animation: PanelAnimation
    ): ActionButton {
        val action = object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                val isCollapsed = isCollapsedGetter()
                projectSettings.isOptionsCollapsed = !isCollapsed
                
                val startHeight = panel.height
                val endHeight = if (isCollapsed) contentPanel.preferredSize.height else 0
                
                animation.animate(startHeight, endHeight) {
                    updateCollapseButtonIcon(!isCollapsed)
                }
            }
        }
        
        val presentation = Presentation(title).apply {
            icon = if (isCollapsedGetter()) AllIcons.General.ArrowRight else AllIcons.General.ArrowDown
            description = if (isCollapsedGetter()) "Show $title" else "Hide $title"
        }
        
        return ActionButton(action, presentation, "Aider${title}Button", ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE)
    }
    
    private fun updateCollapseButtonIcon(collapsed: Boolean) {
        collapseButton.presentation.apply {
            icon = if (collapsed) AllIcons.General.ArrowRight else AllIcons.General.ArrowDown
            description = if (collapsed) "Show Options" else "Hide Options"
        }
    }

    private val collapseButton: ActionButton = createCollapseButton(
        "Options",
        projectSettings::isOptionsCollapsed,
        optionsPanel,
        flagAndArgsPanel,
        panelAnimation
    )

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
        setOKButtonText("OK")
        setCancelButtonText("Cancel")
        setupKeyBindings()
        llmComboBox.selectedItem = settings.llm
        llmComboBox.renderer = LlmComboBoxRenderer(apiKeyChecker, llmComboBox, llmOptions)

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
        historyComboBox.navigateHistory(direction)
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

        val optionsCollapsible = CollapsiblePanel(
            "Additional Options",
            projectSettings::isOptionsCollapsed,
            flagAndArgsPanel
        )
        
        topPanel.add(optionsCollapsible.headerPanel.apply { 
            border = JBUI.Borders.empty(2) 
        }, gbc.apply { gridy++ })
        topPanel.add(optionsCollapsible.contentPanel, gbc.apply { gridy++ })

        // Context view with collapsible UI
        val contextViewPanel = AiderContextViewPanel(project, aiderContextView)
        val contextCollapsible = CollapsiblePanel(
            "Context Files",
            projectSettings::isContextCollapsed,
            contextViewPanel
        )

        val contextPanel = JPanel(BorderLayout()).apply {
            add(contextCollapsible.headerPanel.apply { 
                border = JBUI.Borders.empty(2) 
            }, BorderLayout.NORTH)
            add(contextCollapsible.contentPanel, BorderLayout.CENTER)
            border = JBUI.Borders.empty(5)
        }

        // Add all components to main panel
        panel.add(topPanel, BorderLayout.CENTER)
        panel.add(contextPanel, BorderLayout.SOUTH)


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
        return JPanel(GridBagLayout()).apply {
            val gbc = GridBagConstraints().apply {
                fill = GridBagConstraints.NONE
                anchor = GridBagConstraints.WEST
                insets = JBUI.insets(0, 2)
            }
            
            // LLM selection
            add(JLabel("LLM:").apply {
                displayedMnemonic = KeyEvent.VK_L
                labelFor = llmComboBox
                toolTipText = "Select the Language Model to use"
            }, gbc.apply {
                gridx = 0
                gridy = 0
            })
            add(llmComboBox.apply {
                preferredSize = Dimension(150, preferredSize.height)
            }, gbc.apply {
                gridx = 1
                gridy = 0
                weightx = 0.3
            })
            
            // Yes flag
            add(yesCheckBox.apply {
                mnemonic = KeyEvent.VK_Y
            }, gbc.apply {
                gridx = 2
                gridy = 0
                insets.left = 10
            })
            
            // Additional args
            add(JLabel("Args:").apply {
                displayedMnemonic = KeyEvent.VK_A
                labelFor = additionalArgsField
                toolTipText = "Additional arguments for the Aider command"
            }, gbc.apply {
                gridx = 3
                gridy = 0
                insets.left = 10
            })
            add(additionalArgsField.apply {
                preferredSize = Dimension(200, preferredSize.height)
            }, gbc.apply {
                gridx = 4
                gridy = 0
                weightx = 0.7
                fill = GridBagConstraints.HORIZONTAL
            })
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

