package de.andrena.codingaider.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.utils.ApiKeyManager
import de.andrena.codingaider.utils.DefaultApiKeyChecker
import javax.swing.JComponent

class CustomLlmProviderEditorDialog(
    private val existingProvider: CustomLlmProvider? = null
) : DialogWrapper(null) {

    private val nameField = JBTextField()
    private val baseUrlField = JBTextField()
    private val modelNameField = JBTextField()
    private val displayNameField = JBTextField()
    private val apiKeyField = JBPasswordField()
    private lateinit var baseUrlRow: Row
    private lateinit var apiKeyRow: Row
    private val providerTypeComboBox = com.intellij.openapi.ui.ComboBox(LlmProviderType.values())

    init {
        title = if (existingProvider == null) "Add Custom LLM Provider" else "Edit Custom LLM Provider"
        init()
        
        // Add document listeners to trigger validation on input changes
        nameField.document.addDocumentListener(createValidationListener())
        baseUrlField.document.addDocumentListener(createValidationListener())
        modelNameField.document.addDocumentListener(createValidationListener())
        apiKeyField.document.addDocumentListener(createValidationListener())
        
        providerTypeComboBox.addActionListener {
            updateProviderTypeUI()
        }
        
        // Set initial values
        if (existingProvider != null) {
            nameField.text = existingProvider.name
            baseUrlField.text = existingProvider.baseUrl
            modelNameField.text = existingProvider.modelName
            displayNameField.text = existingProvider.displayName ?: ""
            providerTypeComboBox.selectedItem = existingProvider.type
            
            // Retrieve and mask existing API key
            ApiKeyManager.getCustomModelKey(existingProvider.name)?.let {
                apiKeyField.text = it
            }
        }
        
        // Update UI after setting initial values
        updateProviderTypeUI()
    }


    private fun updateProviderTypeUI() {
        val selectedType = providerTypeComboBox.selectedItem as LlmProviderType
        
        when (selectedType) {
            LlmProviderType.OLLAMA -> {
                if (existingProvider == null) {
                    baseUrlField.text = "http://127.0.0.1:11434"
                }
                baseUrlField.isEnabled = true
                baseUrlField.toolTipText = "Ollama server URL (default: http://127.0.0.1:11434)"
                apiKeyField.isEnabled = false
                if (existingProvider == null) {
                    apiKeyField.text = ""
                }
            }
                LlmProviderType.OPENROUTER -> {
                    if (existingProvider == null) {
                        baseUrlField.text = "https://openrouter.ai/api/v1"
                    }
                    baseUrlField.isEnabled = true
                    baseUrlField.toolTipText = "OpenRouter API base URL"
                    apiKeyField.isEnabled = true
                    apiKeyField.toolTipText = "OpenRouter API key (required)"
                }
                LlmProviderType.OPENAI -> {
                    if (existingProvider == null) {
                        baseUrlField.text = "https://api.openai.com/v1"
                    }
                    baseUrlField.isEnabled = true
                    baseUrlField.toolTipText = "OpenAI API base URL"
                    apiKeyField.isEnabled = true
                    apiKeyField.toolTipText = "OpenAI API key (required)"
                }
                else -> {
                    baseUrlField.isEnabled = true
                    baseUrlField.toolTipText = "API endpoint URL"
                    apiKeyField.isEnabled = true
                    apiKeyField.toolTipText = "API key"
                }
            }

        // Update row visibility based on provider requirements
        baseUrlRow.visible(selectedType.requiresBaseUrl)
        apiKeyRow.visible(selectedType.requiresApiKey)
    }

    override fun createCenterPanel(): JComponent = panel {
        row("Provider Name:") {
            cell(nameField)
                .columns(30)
                .comment("A unique identifier for this provider")
                .focused()
        }
        row("Provider Type:") {
            cell(providerTypeComboBox)
                .columns(30)
                .comment("Select the type of LLM provider")
        }
        baseUrlRow = row("Base URL:") {
            cell(baseUrlField)
                .columns(30)
                .comment("The API endpoint URL (required for OpenAI and Ollama)")
        }
        row("Model Name:") {
            cell(modelNameField)
                .columns(30)
                .comment("The name of the model to use")
        }
        row("Display Name:") {
            cell(displayNameField)
                .columns(30)
                .comment("Optional: A friendly name to show in the UI")
        }
        apiKeyRow = row("API Key:") {
            cell(apiKeyField)
                .columns(30)
                .comment("Optional: Secure API key for the provider")
        }
    }

    override fun doValidate(): ValidationInfo? {
        if (nameField.text.isBlank()) {
            return ValidationInfo("Provider name is required", nameField)
        }
        if (modelNameField.text.isBlank()) {
            return ValidationInfo("Model name is required", modelNameField)
        }
        
        val selectedType = providerTypeComboBox.selectedItem as LlmProviderType
        
        // Validate provider name uniqueness
        val service = service<CustomLlmProviderService>()
        val apiKeyChecker = service<DefaultApiKeyChecker>()
        val normalizedName = nameField.text.trim()
        
        // Check against existing custom providers
        val existingCustomProvider = service.getAllProviders().find { 
            it.name.trim().equals(normalizedName, ignoreCase = true) && 
            it != existingProvider 
        }
        if (existingCustomProvider != null) {
            return ValidationInfo("Provider name must be unique", nameField)
        }
        
        // Check against standard LLM keys
        val standardKeys = apiKeyChecker.getAllStandardLlmKeys()
        if (standardKeys.contains(normalizedName)) {
            return ValidationInfo("Provider name cannot match standard LLM keys", nameField)
        }
        
        // Validate base URL for providers that require it
        if (selectedType.requiresBaseUrl) {
            val baseUrl = baseUrlField.text.trim()
            if (baseUrl.isBlank()) {
                return ValidationInfo("Base URL is required for ${selectedType.name}", baseUrlField)
            }
            if (!isValidUrl(baseUrl)) {
                return ValidationInfo("Invalid URL format", baseUrlField)
            }
        }
        
        // Validate API key for providers that require it
        if (selectedType.requiresApiKey) {
            val apiKeyText = String(apiKeyField.password)
            // For new providers or when a new key is being set
            if (existingProvider == null || apiKeyText != "*".repeat(16)) {
                if (apiKeyText.isBlank()) {
                    return ValidationInfo("API key is required for ${selectedType.name}", apiKeyField)
                }
            }
        }
        
        return null
    }

    private fun isValidUrl(url: String): Boolean {
        return url.matches(Regex("^https?://[^\\s/$.?#].[^\\s]*$"))
    }
    
    private fun createValidationListener() = object : javax.swing.event.DocumentListener {
        override fun insertUpdate(e: javax.swing.event.DocumentEvent) = revalidate()
        override fun removeUpdate(e: javax.swing.event.DocumentEvent) = revalidate()
        override fun changedUpdate(e: javax.swing.event.DocumentEvent) = revalidate()
        
        private fun revalidate() {
            isOKActionEnabled = true  // Reset OK button state
        }
    }

    fun getProvider(): CustomLlmProvider {
        val type: LlmProviderType = providerTypeComboBox.selectedItem as LlmProviderType
        val provider = CustomLlmProvider(
            name = nameField.text,
            displayName = displayNameField.text.takeIf { it.isNotBlank() },
            type = type,
            baseUrl = baseUrlField.text,
            modelName = modelNameField.text,
        )
        
        // Save API key if provided and not masked, except for Ollama
        if (type.requiresApiKey) {
            val apiKeyText = String(apiKeyField.password)
            if (apiKeyText.isNotBlank() && apiKeyText != "*".repeat(16)) {
                // New key entered
                ApiKeyManager.saveCustomModelKey(provider.name, apiKeyText)
            } else if (existingProvider != null) {
                // Preserve existing key if no new key is entered
                val existingKey = ApiKeyManager.getCustomModelKey(existingProvider.name)
                if (existingKey != null) {
                    // If the existing provider had a key, save it for the new provider
                    ApiKeyManager.saveCustomModelKey(provider.name, existingKey)
                }
            }
        }
        
        return provider
    }

}
