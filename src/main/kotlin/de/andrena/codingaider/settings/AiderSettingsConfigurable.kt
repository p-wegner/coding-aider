package de.andrena.codingaider.settings

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.*
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.CommandObserver
import de.andrena.codingaider.inputdialog.PersistentFileManager
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.ApiKeyManager
import de.andrena.codingaider.utils.DefaultApiKeyChecker
import java.awt.Component
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class AiderSettingsConfigurable : Configurable {
    constructor() {
        this.apiKeyChecker = DefaultApiKeyChecker()
        this.useYesFlagCheckBox = JBCheckBox("Use --yes flag by default")
        this.llmOptions = apiKeyChecker.getAllLlmOptions().toTypedArray()
        this.llmComboBox = object : JComboBox<String>(llmOptions) {
            override fun getToolTipText(): String? {
                val selectedItem = selectedItem as? String ?: return null
                return if (apiKeyChecker.isApiKeyAvailableForLlm(selectedItem)) {
                    "API key found for $selectedItem"
                } else {
                    "API key not found for $selectedItem"
                }
            }
        }
        this.additionalArgsField = JBTextField()
        this.isShellModeCheckBox = JBCheckBox("Use Shell Mode by default")
        this.lintCmdField = JBTextField()
        this.showGitComparisonToolCheckBox = JBCheckBox("Show git comparison tool after execution")
        this.activateIdeExecutorAfterWebcrawlCheckBox = JBCheckBox("Activate Post web crawl LLM cleanup (Experimental)")
        this.webCrawlLlmComboBox = ComboBox(apiKeyChecker.getAllLlmOptions().toTypedArray())
        this.deactivateRepoMapCheckBox = JBCheckBox("Deactivate Aider's repo map (--map-tokens 0)")
        this.editFormatComboBox = ComboBox(arrayOf("", "whole", "diff", "whole-func", "diff-func"))
        this.verboseCommandLoggingCheckBox = JBCheckBox("Enable verbose Aider command logging")
        this.useDockerAiderCheckBox = JBCheckBox("Use aider in Docker")
        this.enableMarkdownDialogAutocloseCheckBox = JBCheckBox("Automatically close Output Dialog")
        this.markdownDialogAutocloseDelayField = JBTextField()
        this.mountAiderConfInDockerCheckBox = JBCheckBox("Mount Aider configuration file in Docker")
        this.includeChangeContextCheckBox = JBCheckBox("Include change context in commit messages")
        this.autoCommitsComboBox = ComboBox(arrayOf("Default", "On", "Off"))
        this.dirtyCommitsComboBox = ComboBox(arrayOf("Default", "On", "Off"))
        this.useStructuredModeCheckBox = JBCheckBox("Use Structured Mode")
        this.apiKeyFields = mutableMapOf<String, JPasswordField>()

    }

    private var apiKeyChecker: ApiKeyChecker

    private var settingsComponent: JPanel? = null
    private val useYesFlagCheckBox: JBCheckBox
    private val llmOptions: Array<String>
    private val llmComboBox: JComboBox<String>
    private val additionalArgsField: JBTextField
    private val isShellModeCheckBox: JBCheckBox
    private val lintCmdField: JBTextField
    private val showGitComparisonToolCheckBox: JBCheckBox
    private val activateIdeExecutorAfterWebcrawlCheckBox: JBCheckBox
    private val webCrawlLlmComboBox: ComboBox<String>
    private val deactivateRepoMapCheckBox: JBCheckBox
    private val editFormatComboBox: ComboBox<String>
    private val verboseCommandLoggingCheckBox: JBCheckBox
    private val useDockerAiderCheckBox: JBCheckBox
    private val enableMarkdownDialogAutocloseCheckBox: JBCheckBox
    private val markdownDialogAutocloseDelayField: JBTextField
    private val mountAiderConfInDockerCheckBox: JBCheckBox
    private val includeChangeContextCheckBox: JBCheckBox
    private val autoCommitsComboBox: ComboBox<String>
    private val dirtyCommitsComboBox: ComboBox<String>
    private val useStructuredModeCheckBox: JBCheckBox
    private val apiKeyFields: MutableMap<String, JPasswordField>
    override fun getDisplayName(): String = "Aider"

    override fun createComponent(): JComponent {
        settingsComponent = panel {
            group("Aider Setup") {
                group("API Keys") {
                    apiKeyChecker.getAllApiKeyNames().forEach { keyName ->
                        row(keyName) {
                            val field = JPasswordField()
                            apiKeyFields[keyName] = field
                            cell(field)
                                .resizableColumn()
                                .align(Align.FILL)
                            val saveButton = JButton("Save")
                            saveButton.addActionListener {
                                val apiKey = String(field.password)
                                if (apiKey.isNotEmpty()) {
                                    ApiKeyManager.saveApiKey(keyName, apiKey)
                                    Messages.showInfoMessage("API key for $keyName has been saved.", "API Key Saved")
                                    updateApiKeyField(keyName, field, saveButton)
                                }
                            }
                            cell(saveButton)

                            button("Clear") {
                                ApiKeyManager.removeApiKey(keyName)
                                clearApiKeyField(keyName, field, saveButton)
                                Messages.showInfoMessage(
                                    "API key for $keyName has been cleared from the credential store (if any has been stored). You can now enter a new key. This will be used if defined, otherwise the key from environment or .env files will be used.",
                                    "API Key Cleared"
                                )
                            }
                            updateApiKeyField(keyName, field, saveButton)

                            field.document.addDocumentListener(object : DocumentListener {
                                override fun insertUpdate(e: DocumentEvent) = updateSaveButton()
                                override fun removeUpdate(e: DocumentEvent) = updateSaveButton()
                                override fun changedUpdate(e: DocumentEvent) = updateSaveButton()

                                fun updateSaveButton() {
                                    saveButton.isEnabled = field.password.isNotEmpty() &&
                                            !apiKeyChecker.isApiKeyAvailable(keyName)
                                }
                            })
                        }
                    }
                }
                row {
                    cell(useDockerAiderCheckBox)
                        .component
                        .apply {
                            toolTipText =
                                "If enabled, Aider will be run using the Docker image paulgauthier/aider. Currently a new container will be used for every command, which may delay the execution compared to native aider setup."
                        }
                }

                row {
                    button("Test Aider Installation") {
                        showTestCommandResult()
                    }
                }
            }

            group("General Settings") {
                row { cell(useYesFlagCheckBox) }
                row("Default LLM Model:") {
                    cell(llmComboBox).component.apply {
                        renderer = LlmComboBoxRenderer()
                    }
                }
                row("Default Additional Arguments:") {
                    cell(additionalArgsField)
                        .resizableColumn()
                        .align(Align.FILL)
                    link("Aider options documentation") {
                        BrowserUtil.browse("https://aider.chat/docs/config/options.html")
                    }
                }
                row { cell(isShellModeCheckBox) }
            }

            group("Code Modification Settings") {
                row("Lint Command:") {
                    cell(lintCmdField)
                        .resizableColumn()
                        .align(Align.FILL)
                        .apply {
                            component.toolTipText = "The lint command will be executed after every code change by Aider"
                        }
                }
                row("Edit Format:") {
                    cell(editFormatComboBox)
                        .component
                        .apply {
                            toolTipText =
                                "Select the default edit format for Aider. Leave empty to use the default format for the used LLM."
                        }
                }
            }

            group("Git Settings") {
                row { cell(showGitComparisonToolCheckBox) }
                row("Auto-commits:") {
                    cell(autoCommitsComboBox)
                        .component
                        .apply {
                            toolTipText =
                                "Default: Use system setting. On: Aider will automatically commit changes after each successful edit. Off: Disable auto-commits."
                        }
                }
                row("Dirty-commits:") {
                    cell(dirtyCommitsComboBox)
                        .component
                        .apply {
                            toolTipText =
                                "Default: Use system setting. On: Aider will allow commits even when there are uncommitted changes in the repo. Off: Disable dirty-commits."
                        }
                }
                row {
                    cell(includeChangeContextCheckBox)
                        .component
                        .apply {
                            toolTipText =
                                "If enabled, the commit messages will include the user prompt and affected files."
                        }
                }

            }

            group("Advanced Settings") {
                row {
                    cell(useStructuredModeCheckBox)
                        .component
                        .apply {
                            toolTipText = "If enabled, Aider will create detailed descriptions of changes instead of directly modifying code."
                        }
                }
                row {
                    cell(activateIdeExecutorAfterWebcrawlCheckBox)
                        .component
                        .apply {
                            toolTipText = "This option prompts Aider to clean up the crawled markdown. " +
                                    "Note that this experimental feature may exceed the LLM's token limit and potentially leads to high costs. " +
                                    "Use it with caution."
                        }
                    cell(webCrawlLlmComboBox)
                        .label("Web Crawl LLM:")
                }
                row {
                    cell(deactivateRepoMapCheckBox)
                        .component
                        .apply {
                            toolTipText =
                                "This will deactivate Aider's repo map. Saves time for repo updates, but will give aider less context."
                        }
                }
                row {
                    cell(verboseCommandLoggingCheckBox)
                        .component
                        .apply {
                            toolTipText =
                                "If enabled, Aider command details will be logged in the dialog shown to the user. This may show sensitive information."
                        }
                }
                row {
                    cell(enableMarkdownDialogAutocloseCheckBox)
                        .component
                        .apply {
                            toolTipText = "If enabled, the Output Dialog will automatically close after the specified delay."
                            addItemListener { e ->
                                markdownDialogAutocloseDelayField.isEnabled = e.stateChange == java.awt.event.ItemEvent.SELECTED
                            }
                        }
                    label("Autoclose delay (seconds):")
                    cell(markdownDialogAutocloseDelayField)
                        .component
                        .apply {
                            toolTipText = "Specify the delay in seconds before the Output Dialog closes automatically. Set to 0 for immediate closing."
                            isEnabled = enableMarkdownDialogAutocloseCheckBox.isSelected
                        }
                }
                row {
                    cell(mountAiderConfInDockerCheckBox)
                        .component
                        .apply {
                            toolTipText =
                                "If enabled, the Aider configuration file will be mounted in the Docker container."
                        }
                }
            }

        }.apply {
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }

        return settingsComponent!!
    }

    override fun isModified(): Boolean {
        val settings = AiderSettings.getInstance()
        return useYesFlagCheckBox.isSelected != settings.useYesFlag ||
                llmComboBox.selectedItem as String != settings.llm ||
                additionalArgsField.text != settings.additionalArgs ||
                isShellModeCheckBox.isSelected != settings.isShellMode ||
                lintCmdField.text != settings.lintCmd ||
                showGitComparisonToolCheckBox.isSelected != settings.showGitComparisonTool ||
                activateIdeExecutorAfterWebcrawlCheckBox.isSelected != settings.activateIdeExecutorAfterWebcrawl ||
                webCrawlLlmComboBox.selectedItem as String != settings.webCrawlLlm ||
                deactivateRepoMapCheckBox.isSelected != settings.deactivateRepoMap ||
                editFormatComboBox.selectedItem as String != settings.editFormat ||
                verboseCommandLoggingCheckBox.isSelected != settings.verboseCommandLogging ||
                useDockerAiderCheckBox.isSelected != settings.useDockerAider ||
                enableMarkdownDialogAutocloseCheckBox.isSelected != settings.enableMarkdownDialogAutoclose ||
                markdownDialogAutocloseDelayField.text.toIntOrNull() != settings.markdownDialogAutocloseDelay ||
                mountAiderConfInDockerCheckBox.isSelected != settings.mountAiderConfInDocker ||
                includeChangeContextCheckBox.isSelected != settings.includeChangeContext ||
                autoCommitsComboBox.selectedIndex != settings.autoCommits.toIndex() ||
                dirtyCommitsComboBox.selectedIndex != settings.dirtyCommits.toIndex() ||
                useStructuredModeCheckBox.isSelected != settings.useStructuredMode
    }

    override fun apply() {
        val settings = AiderSettings.getInstance()
        settings.useYesFlag = useYesFlagCheckBox.isSelected
        settings.llm = llmComboBox.selectedItem as String
        settings.additionalArgs = additionalArgsField.text
        settings.isShellMode = isShellModeCheckBox.isSelected
        settings.lintCmd = lintCmdField.text
        settings.showGitComparisonTool = showGitComparisonToolCheckBox.isSelected
        settings.activateIdeExecutorAfterWebcrawl = activateIdeExecutorAfterWebcrawlCheckBox.isSelected
        settings.webCrawlLlm = webCrawlLlmComboBox.selectedItem as String
        settings.deactivateRepoMap = deactivateRepoMapCheckBox.isSelected
        settings.editFormat = editFormatComboBox.selectedItem as String
        settings.verboseCommandLogging = verboseCommandLoggingCheckBox.isSelected
        settings.useDockerAider = useDockerAiderCheckBox.isSelected
        settings.enableMarkdownDialogAutoclose = enableMarkdownDialogAutocloseCheckBox.isSelected
        settings.markdownDialogAutocloseDelay = markdownDialogAutocloseDelayField.text.toIntOrNull() ?: AiderDefaults.MARKDOWN_DIALOG_AUTOCLOSE_DELAY_IN_S
        settings.mountAiderConfInDocker = mountAiderConfInDockerCheckBox.isSelected
        settings.includeChangeContext = includeChangeContextCheckBox.isSelected
        settings.autoCommits = AiderSettings.AutoCommitSetting.fromIndex(autoCommitsComboBox.selectedIndex)
        settings.dirtyCommits = AiderSettings.DirtyCommitSetting.fromIndex(dirtyCommitsComboBox.selectedIndex)
        settings.useStructuredMode = useStructuredModeCheckBox.isSelected
    }


    override fun reset() {
        val settings = AiderSettings.getInstance()
        useYesFlagCheckBox.isSelected = settings.useYesFlag
        llmComboBox.selectedItem = settings.llm
        additionalArgsField.text = settings.additionalArgs
        isShellModeCheckBox.isSelected = settings.isShellMode
        lintCmdField.text = settings.lintCmd
        showGitComparisonToolCheckBox.isSelected = settings.showGitComparisonTool
        activateIdeExecutorAfterWebcrawlCheckBox.isSelected = settings.activateIdeExecutorAfterWebcrawl
        webCrawlLlmComboBox.selectedItem = settings.webCrawlLlm
        deactivateRepoMapCheckBox.isSelected = settings.deactivateRepoMap
        editFormatComboBox.selectedItem = settings.editFormat
        verboseCommandLoggingCheckBox.isSelected = settings.verboseCommandLogging
        useDockerAiderCheckBox.isSelected = settings.useDockerAider
        enableMarkdownDialogAutocloseCheckBox.isSelected = settings.enableMarkdownDialogAutoclose
        markdownDialogAutocloseDelayField.text = settings.markdownDialogAutocloseDelay.toString()
        mountAiderConfInDockerCheckBox.isSelected = settings.mountAiderConfInDocker
        includeChangeContextCheckBox.isSelected = settings.includeChangeContext
        autoCommitsComboBox.selectedIndex = settings.autoCommits.toIndex()
        dirtyCommitsComboBox.selectedIndex = settings.dirtyCommits.toIndex()
        useStructuredModeCheckBox.isSelected = settings.useStructuredMode

        apiKeyFields.forEach { (keyName, field) ->
            field.text = getApiKeyDisplayValue(keyName)
        }
    }

    private fun getApiKeyDisplayValue(keyName: String): String {
        return if (apiKeyChecker.isApiKeyAvailable(keyName)) {
            if (ApiKeyManager.getApiKey(keyName) != null) {
                "*".repeat(16) // Censored placeholder for stored API key
            } else {
                "*An API key is available from another source*" // Placeholder for key from env or .env file
            }
        } else {
            ""
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



    private fun showTestCommandResult() {
        val textArea = JBTextArea().apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }
        val scrollPane = JBScrollPane(textArea)
        scrollPane.preferredSize = java.awt.Dimension(600, 400)

        val dialog = DialogBuilder().apply {
            setTitle("Aider Test Command Result")
            setCenterPanel(scrollPane)
            addOkAction()
        }

        // Show the dialog immediately
        ApplicationManager.getApplication().invokeLater {
            dialog.show()
        }

        // Use SwingWorker to run the command in the background
        val worker = object : SwingWorker<String, String>() {
            override fun doInBackground(): String {
                val observer = object : CommandObserver {
                    override fun onCommandStart(command: String) {
                        publish("Starting command...\n")
                    }

                    override fun onCommandProgress(output: String, runningTime: Long) {
                        publish(output)
                    }

                    override fun onCommandComplete(output: String, exitCode: Int) {
                        publish("\nCommand completed with exit code: $exitCode\n")
                    }

                    override fun onCommandError(error: String) {
                        publish("\nError: $error\n")
                    }
                }

                // Pass the current state of the Docker checkbox
                AiderTestCommand().execute(observer, useDockerAiderCheckBox.isSelected)
                return "Command execution finished."
            }

            override fun process(chunks: List<String>) {
                ApplicationManager.getApplication().invokeLater {
                    textArea.text = "" // Clear the text area before appending new content
                    chunks.forEach { textArea.append(it) }
                    textArea.caretPosition = textArea.document.length
                }
            }

            override fun done() {
                // The dialog is already shown, so we don't need to do anything here
            }
        }

        worker.execute()
    }

    private fun updateApiKeyField(keyName: String, field: JPasswordField, saveButton: JButton) {
        when {
            ApiKeyManager.getApiKey(keyName) != null -> {
                field.text = "*".repeat(16)
                field.isEditable = false
                field.toolTipText = "An API key for $keyName is stored. Clear it first to enter a new one."
                saveButton.isEnabled = false
            }

            apiKeyChecker.isApiKeyAvailable(keyName) -> {
                field.text = "*".repeat(16)
                field.isEditable = false
                field.toolTipText =
                    "An API key for $keyName is available from environment or .env file. You can enter a new one to use after clearing the field. Env files will not be modified."
                saveButton.isEnabled = false
            }

            else -> {
                field.text = ""
                field.isEditable = true
                field.toolTipText = "Enter an API key for $keyName"
                saveButton.isEnabled = false
            }
        }

        field.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = updateSaveButton()
            override fun removeUpdate(e: DocumentEvent) = updateSaveButton()
            override fun changedUpdate(e: DocumentEvent) = updateSaveButton()

            fun updateSaveButton() {
                saveButton.isEnabled = field.password.isNotEmpty() &&
                        String(field.password) != "*".repeat(16) &&
                        String(field.password) != "*An API key is available from another source*"
            }
        })
    }

    private fun clearApiKeyField(keyName: String, field: JPasswordField, saveButton: JButton) {
        field.text = ""
        field.isEditable = true
        field.toolTipText = "Enter a new API key for $keyName"
        saveButton.isEnabled = false

        field.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = updateSaveButton()
            override fun removeUpdate(e: DocumentEvent) = updateSaveButton()
            override fun changedUpdate(e: DocumentEvent) = updateSaveButton()

            fun updateSaveButton() {
                saveButton.isEnabled = field.password.isNotEmpty()
            }
        })
    }

    private fun updateApiKeyFieldsOnClose() {
        apiKeyFields.forEach { (keyName, field) ->
            val isApiKeyAvailable = apiKeyChecker.isApiKeyAvailable(keyName)
            field.isEditable = !isApiKeyAvailable
            if (isApiKeyAvailable) {
                field.text = getApiKeyDisplayValue(keyName)
            }
        }
    }

    override fun disposeUIResources() {
        updateApiKeyFieldsOnClose()
        settingsComponent = null
        // Ensure that any pending changes are saved
        if (isModified) {
            apply()
        }
    }
}
