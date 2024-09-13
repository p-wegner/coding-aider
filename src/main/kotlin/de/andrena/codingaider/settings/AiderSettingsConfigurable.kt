package de.andrena.codingaider.settings

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
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
import java.awt.Component
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class AiderSettingsConfigurable(private val project: Project) : Configurable {
    private var settingsComponent: JPanel? = null
    private val useYesFlagCheckBox = JBCheckBox("Use --yes flag by default")
    private val llmOptions = ApiKeyChecker.getAllLlmOptions().toTypedArray()
    private val llmComboBox = object : JComboBox<String>(llmOptions) {
        override fun getToolTipText(): String? {
            val selectedItem = selectedItem as? String ?: return null
            return if (ApiKeyChecker.isApiKeyAvailableForLlm(selectedItem)) {
                "API key found for $selectedItem"
            } else {
                "API key not found for $selectedItem"
            }
        }
    }
    private val additionalArgsField = JBTextField()
    private val isShellModeCheckBox = JBCheckBox("Use Shell Mode by default")
    private val lintCmdField = JBTextField()
    private val showGitComparisonToolCheckBox = JBCheckBox("Show Git Comparison Tool after execution")
    private val activateIdeExecutorAfterWebcrawlCheckBox =
        JBCheckBox("Activate Post web crawl LLM cleanup (Experimental)")
    private val webCrawlLlmComboBox = ComboBox(ApiKeyChecker.getAllLlmOptions().toTypedArray())
    private val deactivateRepoMapCheckBox = JBCheckBox("Deactivate Aider's repo map (--map-tokens 0)")
    private val editFormatComboBox = ComboBox(arrayOf("", "whole", "diff", "whole-func", "diff-func"))
    private val verboseCommandLoggingCheckBox = JBCheckBox("Enable verbose Aider command logging")
    private val useDockerAiderCheckBox = JBCheckBox("Use Aider in Docker")
    private val enableMarkdownDialogAutocloseCheckBox = JBCheckBox("Enable Output Dialog autoclose")
    private val apiKeyFields = mutableMapOf<String, JPasswordField>()

    override fun getDisplayName(): String = "Aider"

    private val persistentFileManager = PersistentFileManager(project.basePath ?: "")
    private val persistentFilesListModel = DefaultListModel<FileData>()
    private val persistentFilesList = JBList(persistentFilesListModel).apply {
        addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyPressed(e: java.awt.event.KeyEvent) {
                if (e.keyCode == java.awt.event.KeyEvent.VK_DELETE) {
                    removeSelectedFiles()
                }
            }
        })
    }

    override fun createComponent(): JComponent {
        persistentFilesList.cellRenderer = PersistentFileRenderer()
        loadPersistentFiles()
        settingsComponent = panel {
            group("Aider Setup") {
                group("API Keys") {
                    ApiKeyChecker.getAllApiKeyNames().forEach { keyName ->
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
                                    "API key for $keyName has been cleared from the credential store. You can now enter a new key.",
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
                                            !ApiKeyChecker.isApiKeyAvailable(keyName)
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
                    link("Aider Options Documentation") {
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
                row { cell(showGitComparisonToolCheckBox) }
                row("Edit Format:") {
                    cell(editFormatComboBox)
                        .component
                        .apply {
                            toolTipText =
                                "Select the default edit format for Aider. Leave empty to use the default format for the used LLM."
                        }
                }
                row {
                    cell(deactivateRepoMapCheckBox)
                        .component
                        .apply {
                            toolTipText =
                                "This will deactivate Aider's repo map. Saves time for repo updates, but will give aider less context."
                        }
                }
            }

            group("Advanced Settings") {
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
                    cell(verboseCommandLoggingCheckBox)
                        .component
                        .apply {
                            toolTipText =
                                "If enabled, Aider command details will be logged in the dialog shown to the user"
                        }
                }
                row {
                    cell(enableMarkdownDialogAutocloseCheckBox)
                        .component
                        .apply {
                            toolTipText =
                                "If enabled, the Output Dialog will automatically close after a 10-second delay."
                        }
                }
            }

            group("Persistent Files") {
                row {
                    scrollCell(persistentFilesList)
                        .align(Align.FILL)
                        .resizableColumn()
                }
                row {
                    button("Add Files") { addPersistentFiles() }
                    button("Toggle Read-Only") { toggleReadOnlyMode() }
                    button("Remove Files") { removeSelectedFiles() }
                }
            }
        }.apply {
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }

        return settingsComponent!!
    }

    override fun isModified(): Boolean {
        val settings = AiderSettings.getInstance(project)
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
                apiKeyFields.any { (keyName, field) ->
                    val currentValue = String(field.password)
                    currentValue.isNotEmpty() && currentValue != "*".repeat(16) &&
                            currentValue != (ApiKeyManager.getApiKey(keyName) ?: "")
                }
    }

    override fun apply() {
        val settings = AiderSettings.getInstance(project)
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

        apiKeyFields.forEach { (keyName, field) ->
            val enteredValue = String(field.password)
            when {
                enteredValue.isNotEmpty() && enteredValue != "*".repeat(16) && 
                enteredValue != "*An API key is available from another source*" -> {
                    ApiKeyManager.saveApiKey(keyName, enteredValue)
                }
                enteredValue.isEmpty() -> {
                    ApiKeyManager.removeApiKey(keyName)
                }
            }
        }
    }

    override fun disposeUIResources() {
        settingsComponent = null
        // Ensure that any pending changes are saved
        if (isModified) {
            apply()
        }
    }


    override fun reset() {
        val settings = AiderSettings.getInstance(project)
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

        apiKeyFields.forEach { (keyName, field) ->
            field.text = getApiKeyDisplayValue(keyName)
        }
    }

    private fun getApiKeyDisplayValue(keyName: String): String {
        return if (ApiKeyChecker.isApiKeyAvailable(keyName)) {
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

    private fun addPersistentFiles() {
        val descriptor = FileChooserDescriptor(true, true, false, false, false, true)
        val files = FileChooser.chooseFiles(descriptor, project, null)
        val fileDataList = files.flatMap { file ->
            if (file.isDirectory) {
                file.children.filter { it.isValid && !it.isDirectory }.map { FileData(it.path, false) }
            } else {
                listOf(FileData(file.path, false))
            }
        }
        persistentFileManager.addAllFiles(fileDataList)
        loadPersistentFiles()
    }

    private fun toggleReadOnlyMode() {
        val selectedFiles = persistentFilesList.selectedValuesList
        selectedFiles.forEach { fileData ->
            val updatedFileData = fileData.copy(isReadOnly = !fileData.isReadOnly)
            persistentFileManager.updateFile(updatedFileData)
        }
        loadPersistentFiles()
    }

    private fun removeSelectedFiles() {
        val selectedFiles = persistentFilesList.selectedValuesList
        persistentFileManager.removePersistentFiles(selectedFiles.map { it.filePath })
        loadPersistentFiles()
    }

    private fun loadPersistentFiles() {
        persistentFilesListModel.clear()
        persistentFileManager.getPersistentFiles().forEach { file ->
            persistentFilesListModel.addElement(file)
        }
    }

    private inner class PersistentFileRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (component is JLabel && value is FileData) {
                component.text = "${value.filePath} ${if (value.isReadOnly) "(Read-Only)" else ""}"
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

        val dialog = DialogBuilder(project).apply {
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
                    override fun onCommandStart(message: String) {
                        publish("Starting command...\n")
                    }

                    override fun onCommandProgress(output: String, runningTime: Long) {
                        publish(output)
                    }

                    override fun onCommandComplete(output: String, exitCode: Int) {
                        publish("\nCommand completed with exit code: $exitCode\n")
                    }

                    override fun onCommandError(errorMessage: String) {
                        publish("\nError: $errorMessage\n")
                    }
                }

                AiderTestCommand(project).execute(observer)
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
            ApiKeyChecker.isApiKeyAvailable(keyName) -> {
                field.text = "*An API key is available from another source*"
                field.isEditable = false
                field.toolTipText = "An API key for $keyName is available from environment or .env file. Clear it first to enter a new one."
                saveButton.isEnabled = false
            }
            else -> {
                field.text = ""
                field.isEditable = true
                field.toolTipText = "Enter an API key for $keyName"
                saveButton.isEnabled = false
            }
        }
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
}
