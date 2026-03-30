package de.andrena.codingaider.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.ui.TextFieldWithHistory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import de.andrena.codingaider.executors.api.CommandObserver
import de.andrena.codingaider.executors.strategies.GithubCopilotCliExecutionStrategy
import de.andrena.codingaider.settings.tabs.SettingsTabPanel
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.ApiKeyManager
import de.andrena.codingaider.utils.CopilotAuthChecker
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class AiderSetupPanel(
    apiKeyChecker: ApiKeyChecker,
    private val dockerModeChanged: (Boolean) -> Unit
) : SettingsTabPanel(apiKeyChecker) {
    private val apiKeyFields = mutableMapOf<String, JPasswordField>()
    val useDockerAiderCheckBox = JBCheckBox("Use aider in Docker")
    val dockerImageField = TextFieldWithHistory()
    val aiderExecutablePathField = TextFieldWithHistory()
    val useCopilotCliCheckBox = JBCheckBox("Use GitHub Copilot CLI instead of Aider")
    val ghExecutablePathField = TextFieldWithHistory()
    val copilotProviderBaseUrlField = JBTextField()
    val copilotProviderTypeField = TextFieldWithHistory()
    private val copilotApiKeyField = JPasswordField()
    
    override fun createPanel(panel: Panel) {
        panel.group("Aider Setup") {
            createApiKeysGroup()
            createDockerSetup()
            createAiderExecutableGroup()
            createCopilotCliGroup()

            row {
                button("Test Aider Installation") {
                    showTestCommandResult()
                }
            }
        }
    }


    private fun Panel.createApiKeysGroup() {
        group("Custom Providers") {
            row {
                button("Manage Providers...") {
                    CustomLlmProviderDialog().show()
                }
            }
        }


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
                }
            }
        }
    }

    private fun Panel.createDockerSetup() {
        row {
            cell(useDockerAiderCheckBox)
                .component
                .apply {
                    toolTipText =
                        "If enabled, Aider will be run using the Docker image paulgauthier/aider. Currently a new container will be used for every command, which may delay the execution compared to native aider setup."
                    addActionListener {
                        dockerModeChanged(isSelected)
                    }
                }
        }
        row("Docker Image:") {
            cell(dockerImageField)
                .resizableColumn()
                .align(Align.FILL)
                .component
                .apply {
                    toolTipText = "Enter the full Docker image name including tag (e.g. paulgauthier/aider:v0.68.0)"
                    setHistory(listOf(AiderDefaults.DOCKER_IMAGE, "paulgauthier/aider:latest","paulgauthier/aider:dev"))
                }

            // Add listener to dynamically enable/disable docker image field
            useDockerAiderCheckBox.addActionListener {
                dockerImageField.isEnabled = useDockerAiderCheckBox.isSelected
            }
        }
        row {
            val warningLabel = JLabel().apply {
                foreground = UIManager.getColor("Label.errorForeground")
                isVisible = false
            }
            cell(warningLabel)
            dockerImageField.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent) = checkTag()
                override fun removeUpdate(e: DocumentEvent) = checkTag()
                override fun changedUpdate(e: DocumentEvent) = checkTag()

                private fun checkTag() {
                    if (dockerImageField.text != AiderDefaults.DOCKER_IMAGE) {
                        warningLabel.text =
                            "Warning: Using a different docker image and version than ${AiderDefaults.DOCKER_IMAGE} " +
                                    "might not be fully compatibility with the plugin."
                        warningLabel.isVisible = true
                    } else {
                        warningLabel.isVisible = false
                    }
                }
            })
        }
    }

    private fun Panel.createAiderExecutableGroup() {
        group("Aider Executable") {
            row("Aider Executable Path:") {
                cell(aiderExecutablePathField)
                    .resizableColumn()
                    .align(Align.FILL)
                    .component
                    .apply {
                        toolTipText =
                            "Only change this if you have a custom aider installation or if aider is not found on your system PATH. Default 'aider' works in most cases."
                        setHistory(listOf(AiderDefaults.AIDER_EXECUTABLE_PATH))
                    }
                button("Reset to Default") {
                    aiderExecutablePathField.text = AiderDefaults.AIDER_EXECUTABLE_PATH
                }
            }
        }
    }

    private fun Panel.createCopilotCliGroup() {
        group("GitHub Copilot CLI") {
            row {
                cell(useCopilotCliCheckBox)
                    .component
                    .apply {
                        toolTipText =
                            "Use GitHub Copilot CLI (gh copilot) as the AI backend instead of Aider. " +
                            "Requires 'gh' CLI v2.89.0+ to be installed. Incompatible with Docker and Sidecar modes."
                    }
            }
            row("gh Executable Path:") {
                cell(ghExecutablePathField)
                    .resizableColumn()
                    .align(Align.FILL)
                    .component
                    .apply {
                        toolTipText =
                            "Path to the 'gh' CLI executable. Leave as 'gh' if it is on your system PATH."
                        setHistory(listOf(AiderDefaults.GH_EXECUTABLE_PATH))
                    }
                button("Reset to Default") {
                    ghExecutablePathField.text = AiderDefaults.GH_EXECUTABLE_PATH
                }
            }
            row {
                button("Check GitHub Auth Status") {
                    val settings = AiderSettings.getInstance()
                    val checker = CopilotAuthChecker(settings)
                    val status = checker.checkAuth()
                    val (title, message) = when (status) {
                        is CopilotAuthChecker.AuthStatus.Authenticated ->
                            "Authenticated" to "Successfully authenticated with GitHub Copilot."
                        is CopilotAuthChecker.AuthStatus.ByokConfigured ->
                            "BYOK Configured" to "Using custom provider (BYOK mode). GitHub authentication not required."
                        is CopilotAuthChecker.AuthStatus.NotAuthenticated ->
                            "Not Authenticated" to status.message
                        is CopilotAuthChecker.AuthStatus.GhNotFound ->
                            "gh CLI Not Found" to status.message
                    }
                    Messages.showInfoMessage(message, title)
                }
            }
            group("Custom Provider (BYOK - optional)") {
                row {
                    label("Leave empty to use GitHub Copilot's default model routing.")
                }
                row("Provider Base URL:") {
                    cell(copilotProviderBaseUrlField)
                        .resizableColumn()
                        .align(Align.FILL)
                        .component
                        .apply {
                            toolTipText =
                                "Set COPILOT_PROVIDER_BASE_URL to use a custom OpenAI-compatible endpoint. " +
                                "Example: http://localhost:11434/v1 for Ollama."
                        }
                }
                row("Provider Type:") {
                    cell(copilotProviderTypeField)
                        .resizableColumn()
                        .align(Align.FILL)
                        .component
                        .apply {
                            toolTipText =
                                "Provider type: 'openai' (default), 'azure', or 'anthropic'."
                            setHistory(listOf("openai", "azure", "anthropic"))
                        }
                }
                row("Provider API Key:") {
                    cell(copilotApiKeyField)
                        .resizableColumn()
                        .align(Align.FILL)
                    val saveKeyButton = JButton("Save")
                    saveKeyButton.addActionListener {
                        val apiKey = String(copilotApiKeyField.password)
                        if (apiKey.isNotEmpty()) {
                            ApiKeyManager.saveApiKey(
                                GithubCopilotCliExecutionStrategy.COPILOT_PROVIDER_API_KEY_STORE,
                                apiKey
                            )
                            Messages.showInfoMessage(
                                "Copilot provider API key has been saved.",
                                "API Key Saved"
                            )
                            copilotApiKeyField.text = "*".repeat(16)
                            copilotApiKeyField.isEditable = false
                            saveKeyButton.isEnabled = false
                        }
                    }
                    cell(saveKeyButton)
                    button("Clear") {
                        ApiKeyManager.removeApiKey(
                            GithubCopilotCliExecutionStrategy.COPILOT_PROVIDER_API_KEY_STORE
                        )
                        copilotApiKeyField.text = ""
                        copilotApiKeyField.isEditable = true
                        Messages.showInfoMessage(
                            "Copilot provider API key has been cleared.",
                            "API Key Cleared"
                        )
                    }
                    updateCopilotApiKeyField(saveKeyButton)
                }
            }
        }
    }

    private fun updateCopilotApiKeyField(saveButton: JButton) {
        val hasStoredKey = ApiKeyManager.getApiKey(
            GithubCopilotCliExecutionStrategy.COPILOT_PROVIDER_API_KEY_STORE
        ) != null
        if (hasStoredKey) {
            copilotApiKeyField.text = "*".repeat(16)
            copilotApiKeyField.isEditable = false
            saveButton.isEnabled = false
        }
        copilotApiKeyField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = updateButton()
            override fun removeUpdate(e: DocumentEvent) = updateButton()
            override fun changedUpdate(e: DocumentEvent) = updateButton()
            private fun updateButton() {
                saveButton.isEnabled = copilotApiKeyField.password.isNotEmpty() &&
                        String(copilotApiKeyField.password) != "*".repeat(16)
            }
        })
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

        ApplicationManager.getApplication().invokeLater {
            dialog.show()
        }

        val worker = object : SwingWorker<String, String>() {
            override fun doInBackground(): String {
                val observer = object : CommandObserver {
                    override fun onCommandStart(message: String) {
                        publish("Starting command...\n")
                    }

                    override fun onCommandProgress(message: String, runningTime: Long) {
                        publish(message)
                    }

                    override fun onCommandComplete(message: String, exitCode: Int) {
                        publish("$message\nCommand completed with exit code: $exitCode\n")
                    }

                    override fun onCommandError(message: String) {
                        publish("\nError: $message\n")
                    }
                }

                AiderTestCommand().execute(observer, useDockerAiderCheckBox.isSelected)
                return "Command execution finished."
            }

            override fun process(chunks: List<String>) {
                ApplicationManager.getApplication().invokeLater {
                    textArea.text = ""
                    chunks.forEach { textArea.append(it) }
                    textArea.caretPosition = textArea.document.length
                }
            }

            override fun done() {
                // Dialog is already shown
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

    fun updateApiKeyFieldsOnClose() {
        apiKeyFields.forEach { (keyName, field) ->
            val isApiKeyAvailable = apiKeyChecker.isApiKeyAvailable(keyName)
            field.isEditable = !isApiKeyAvailable
            if (isApiKeyAvailable) {
                field.text = getApiKeyDisplayValue(keyName)
            }
        }
    }

    private fun getApiKeyDisplayValue(keyName: String): String {
        return if (apiKeyChecker.isApiKeyAvailable(keyName)) {
            if (ApiKeyManager.getApiKey(keyName) != null) {
                "*".repeat(16)
            } else {
                "*An API key is available from another source*"
            }
        } else {
            ""
        }
    }

    override fun getTabName(): String = "Setup"
    
    override fun getTabTooltip(): String = "Configure Aider and API keys"
    
    override fun isModified(): Boolean {
        val settings = AiderSettings.getInstance()
        return useDockerAiderCheckBox.isSelected != settings.useDockerAider ||
                dockerImageField.text != settings.dockerImage ||
                aiderExecutablePathField.text != settings.aiderExecutablePath ||
                useCopilotCliCheckBox.isSelected != settings.useCopilotCli ||
                ghExecutablePathField.text != settings.ghExecutablePath ||
                copilotProviderBaseUrlField.text != settings.copilotProviderBaseUrl ||
                copilotProviderTypeField.text != settings.copilotProviderType
    }


    override fun apply() {
        val settings = AiderSettings.getInstance()
        settings.useDockerAider = useDockerAiderCheckBox.isSelected
        settings.dockerImage = dockerImageField.text
        settings.aiderExecutablePath = aiderExecutablePathField.text
        settings.useCopilotCli = useCopilotCliCheckBox.isSelected
        settings.ghExecutablePath = ghExecutablePathField.text
        settings.copilotProviderBaseUrl = copilotProviderBaseUrlField.text
        settings.copilotProviderType = copilotProviderTypeField.text
    }

    override fun reset() {
        val settings = AiderSettings.getInstance()
        useDockerAiderCheckBox.isSelected = settings.useDockerAider
        dockerImageField.text = settings.dockerImage
        dockerImageField.isEnabled = settings.useDockerAider
        aiderExecutablePathField.text = settings.aiderExecutablePath
        useCopilotCliCheckBox.isSelected = settings.useCopilotCli
        ghExecutablePathField.text = settings.ghExecutablePath.ifBlank { AiderDefaults.GH_EXECUTABLE_PATH }
        copilotProviderBaseUrlField.text = settings.copilotProviderBaseUrl
        copilotProviderTypeField.text = settings.copilotProviderType.ifBlank { AiderDefaults.COPILOT_PROVIDER_TYPE }
        updateApiKeyFieldsOnClose()
    }
}
