package de.andrena.codingaider.inputdialog

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
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
import de.andrena.codingaider.actions.aider.AiderClipboardImageAction
import de.andrena.codingaider.actions.ide.SettingsAction
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.services.AiderDialogStateService
import de.andrena.codingaider.services.PersistentFileService
import de.andrena.codingaider.services.TokenCountService
import de.andrena.codingaider.services.plans.AiderPlanPromptService
import de.andrena.codingaider.settings.AiderProjectSettings
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import de.andrena.codingaider.settings.LlmSelection
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.DefaultApiKeyChecker
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.KeyEvent
import javax.swing.*


private const val PROMPT_LABEL = "Enter your prompt:"

class AiderInputDialog(
    val project: Project,
    files: List<FileData>,
    initialText: String = "",
    private val apiKeyChecker: ApiKeyChecker = service<DefaultApiKeyChecker>()
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
            // Set up transfer handler for both the text field and its editor
            val clipboardAction = AiderClipboardImageAction()
            val imageHandler = { image: java.awt.Image -> 
                clipboardAction.saveImageFromPaste(project, image)
            }
            
            // Create a single instance of ImageAwareTransferHandler to be reused
            val imageAwareHandler = ImageAwareTransferHandler(this.transferHandler, imageHandler)
            
            // Set handler on the text field itself
            this.transferHandler = imageAwareHandler
            
            // Also set handler on the editor component when available
            this.getEditor(true)?.let { editor ->
                TextCompletionUtil.installCompletionHint(editor)
                editor.component.transferHandler = imageAwareHandler
                
                // Add visual feedback during drag operations
                editor.component.dropTarget = java.awt.dnd.DropTarget(editor.component, 
                    object : java.awt.dnd.DropTargetAdapter() {
                        override fun dragEnter(dtde: java.awt.dnd.DropTargetDragEvent) {
                            if (dtde.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.imageFlavor) ||
                                dtde.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor)) {
                                editor.component.border = javax.swing.BorderFactory.createLineBorder(
                                    java.awt.Color.BLUE, 2)
                            }
                        }
                        
                        override fun dragExit(dte: java.awt.dnd.DropTargetEvent) {
                            editor.component.border = null
                        }
                        
                        override fun drop(dtde: java.awt.dnd.DropTargetDropEvent) {
                            editor.component.border = null
                            dtde.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY)
                        }
                    }
                )
                
                // Add visual feedback during drag operations
                editor.component.dropTarget = java.awt.dnd.DropTarget(editor.component, 
                    object : java.awt.dnd.DropTargetAdapter() {
                        override fun dragEnter(dtde: java.awt.dnd.DropTargetDragEvent) {
                            if (dtde.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.imageFlavor) ||
                                dtde.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor)) {
                                editor.component.border = javax.swing.BorderFactory.createLineBorder(
                                    java.awt.Color.BLUE, 2)
                            }
                        }
                        
                        override fun dragExit(dte: java.awt.dnd.DropTargetEvent) {
                            editor.component.border = null
                        }
                        
                        override fun drop(dtde: java.awt.dnd.DropTargetDropEvent) {
                            editor.component.border = null
                            dtde.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY)
                        }
                    }
                )
            }
            val value: DocumentListener = object : DocumentListener {
                override fun documentChanged(e: DocumentEvent) = updateTokenCount()
            }
            document.addDocumentListener(value)
        }

    private fun updateTokenCount() {
        val totalTokens = tokenCountService.countTokensInText(getInputText()) + allFileTokens
        tokenCountLabel.text = "$totalTokens tokens"
    }

    val lazyCacheDelegate = LazyCacheDelegate {
        tokenCountService.countTokensInFiles(getAllFiles().filterNot { tokenCountService.shouldSkipTokenCount(it.filePath) })
    }
    val allFileTokens by lazyCacheDelegate


    private val projectSettings = AiderProjectSettings.getInstance(project)
    private val optionsPanel = AiderOptionsPanel(apiKeyChecker = apiKeyChecker)
    private var initialMode = getInstance().defaultMode

    private var modeSegmentedButton: SegmentedButton<AiderMode>? = null
    fun selectMode(mode: AiderMode) {
        modeSegmentedButton?.selectedItem = mode
    }

    private val modeSegmentedButtonPanel: DialogPanel
    private val messageLabel: JLabel
    private val tokenCountLabel = JLabel("0 tokens").apply {
        toolTipText =
            "The actual token count may vary depending on the model. The displayed number uses GPT-4O encoding as a heuristic."
        foreground = UIManager.getColor("Label.disabledForeground")
        border = JBUI.Borders.empty(2, 5)
        horizontalAlignment = SwingConstants.RIGHT
    }
    private val historyComboBox = AiderHistoryComboBox(project, inputTextField)
    private val aiderContextView: AiderContextView
    private var splitPane: OnePixelSplitter
    private val settingsButton: ActionButton
    private val optionsManager = AiderOptionsManager(project, optionsPanel)

    init {
        title = "Aider Command"
        messageLabel = JLabel(PROMPT_LABEL)

        aiderContextView = AiderContextView(
            project,
            files,
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
        // TODO: introduce proper encapsulation for this
        optionsManager.llmComboBox.selectedItem = settings.llm
        optionsManager.llmComboBox.renderer =
            LlmComboBoxRenderer(apiKeyChecker, optionsManager.llmComboBox, optionsPanel.llmOptions)
        optionsManager.yesCheckBox.isSelected = settings.useYesFlag
        optionsManager.additionalArgsField.text = settings.additionalArgs

        // Set minimum size for the dialog and its components
        inputTextField.minimumSize = Dimension(600, 100)
        aiderContextView.minimumSize = Dimension(600, 200)
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
        val restoreButton = ActionButton(
            object : AnAction() {
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

        // Create a panel to hold input field and token count
        val inputPanel = JPanel(BorderLayout())
        inputPanel.add(inputTextField, BorderLayout.CENTER)
        inputPanel.add(tokenCountLabel, BorderLayout.SOUTH)
        topPanel.add(inputPanel, gbc)

        // Options panel with collapsible UI
        gbc.gridy++
        gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        topPanel.add(optionsManager.panel, gbc.apply { gridy++ })

        // Context view with collapsible UI
        val contextViewPanel = AiderContextViewPanel(project, aiderContextView)
        val contextPanel = panel {
            collapsibleGroup("Context Files") {
                row {
                    cell(contextViewPanel)
                        .align(com.intellij.ui.dsl.builder.Align.FILL)
                }
            }
                .apply { expanded = !projectSettings.isContextCollapsed }
                .addExpandedListener { projectSettings.isContextCollapsed = !it }
        }.apply {
            border = JBUI.Borders.empty(5)
        }

        // Add all components to main panel
        panel.add(topPanel, BorderLayout.CENTER)
        panel.add(contextPanel, BorderLayout.SOUTH)


        return panel
    }

    fun getInputText(): String = inputTextField.text

    fun isYesFlagChecked(): Boolean = optionsManager.yesCheckBox.isSelected
    fun getLlm(): LlmSelection = optionsManager.llmComboBox.selectedItem as LlmSelection
    fun getAdditionalArgs(): String = optionsManager.additionalArgsField.text
    fun getAllFiles(): List<FileData> = aiderContextView?.getAllFiles() ?: emptyList()
    val selectedMode get() = modeSegmentedButton?.selectedItem ?: initialMode
    fun isShellMode(): Boolean = selectedMode == AiderMode.SHELL
    fun isStructuredMode(): Boolean = selectedMode == AiderMode.STRUCTURED

    private fun updateModeUI() {
        inputTextField.isEnabled = selectedMode != AiderMode.SHELL
        messageLabel.text = when (selectedMode) {
            AiderMode.SHELL -> "Shell mode enabled, no prompt required"
            AiderMode.STRUCTURED -> getStructuredModeMessageLabel()
            else -> PROMPT_LABEL
        }
    }

    private fun getStructuredModeMessageLabel(): String {
        val existingPlans =
            project.service<AiderPlanPromptService>()
                .filterPlanRelevantFiles(project.service<PersistentFileService>().getPersistentFiles())
        if (existingPlans.isNotEmpty()) {
            val firstPlan = existingPlans.first()
            val planName = firstPlan.filePath.substringAfterLast("/")
            return "Continue with the plan $planName (message may be left empty):"
        }
        return "Enter feature description that will be used to create a plan:"
    }


    private fun restoreLastState() {
        AiderDialogStateService.getInstance(project).getLastState()?.let { state ->
            inputTextField.text = state.message
            optionsPanel.yesCheckBox.isSelected = state.useYesFlag
            optionsPanel.llmComboBox.selectedItem = state.llm
            optionsPanel.additionalArgsField.text = state.additionalArgs
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

    override fun dispose() {
        optionsPanel.dispose()
        super.dispose()
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

