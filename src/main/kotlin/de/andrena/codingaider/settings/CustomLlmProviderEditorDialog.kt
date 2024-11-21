package de.andrena.codingaider.settings

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.utils.ApiKeyManager
import javax.swing.JComponent

class CustomLlmProviderEditorDialog(
    private val existingProvider: CustomLlmProvider? = null
) : DialogWrapper(null) {

    private val nameField = JBTextField()
    private val baseUrlField = JBTextField()
    private val modelNameField = JBTextField()
    private val displayNameField = JBTextField()
    private val apiKeyField = JBPasswordField()
    private val providerTypeComboBox = com.intellij.openapi.ui.ComboBox(LlmProviderType.values())

    init {
        title = if (existingProvider == null) "Add Custom LLM Provider" else "Edit Custom LLM Provider"
        init()
        
        providerTypeComboBox.addActionListener {
            updateProviderTypeUI()
        }
        
        existingProvider?.let { provider ->
            nameField.text = provider.name
            baseUrlField.text = provider.baseUrl
            modelNameField.text = provider.modelName
            displayNameField.text = provider.displayName ?: ""
            providerTypeComboBox.selectedItem = provider.type
            
            updateProviderTypeUI()
            
            // Retrieve and mask existing API key
            ApiKeyManager.getCustomModelKey(provider.name)?.let { 
                apiKeyField.text = "*".repeat(16)
            }
        }
    }

    private fun updateProviderTypeUI() {
        val selectedType = providerTypeComboBox.selectedItem as LlmProviderType
        
        // Update base URL field visibility and requirement
        baseUrlField.isEnabled = selectedType.requiresBaseUrl
        
        // Update API key row visibility
        if (selectedType == LlmProviderType.OLLAMA) {
            // Remove API key row if it exists
            createCenterPanel()
        }
        
        // Validate base URL requirement
        if (selectedType.requiresBaseUrl) {
            baseUrlField.toolTipText = "The API endpoint URL is required for ${selectedType.name}"
        } else {
            baseUrlField.toolTipText = "Optional base URL for ${selectedType.name}"
        }
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
        row("Base URL:") {
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
        // Only show API key row for providers that require it
        if ((providerTypeComboBox.selectedItem as LlmProviderType) != LlmProviderType.OLLAMA) {
            row("API Key:") {
                cell(apiKeyField)
                    .columns(30)
                    .comment("Optional: Secure API key for the provider")
            }
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
        if (selectedType.requiresBaseUrl && baseUrlField.text.isBlank()) {
            return ValidationInfo("Base URL is required for ${selectedType.name}", baseUrlField)
        }
        
        // Validate API key if it's a new entry or a new key is provided, except for Ollama
        if (selectedType.requiresApiKey) {
            val apiKeyText = String(apiKeyField.password)
            if (existingProvider == null && apiKeyText.isBlank()) {
                return ValidationInfo("API key is required for ${selectedType.name}", apiKeyField)
            }
        }
        
        return null
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
                ApiKeyManager.saveCustomModelKey(provider.name, apiKeyText)
            }
        }
        
        return provider
    }
}
