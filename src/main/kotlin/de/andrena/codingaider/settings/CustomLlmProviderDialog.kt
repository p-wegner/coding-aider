package de.andrena.codingaider.settings

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class CustomLlmProviderDialog(
    private val existingProvider: CustomLlmProvider? = null
) : DialogWrapper(null) {

    private val nameField = JBTextField()
    private val baseUrlField = JBTextField()
    private val modelNameField = JBTextField()
    private val displayNameField = JBTextField()
    private val providerTypeComboBox = com.intellij.openapi.ui.ComboBox(LlmProviderType.values())

    init {
        title = if (existingProvider == null) "Add Custom LLM Provider" else "Edit Custom LLM Provider"
        init()
        existingProvider?.let { provider ->
            nameField.text = provider.name
            baseUrlField.text = provider.baseUrl ?: ""
            modelNameField.text = provider.modelName
            displayNameField.text = provider.displayName ?: ""
            providerTypeComboBox.selectedItem = provider.type
        }
    }

    override fun createCenterPanel(): JComponent = panel {
        row("Provider Name:") {
            cell(nameField)
                .comment("A unique identifier for this provider")
                .focused()
        }
        row("Provider Type:") {
            cell(providerTypeComboBox)
                .comment("Select the type of LLM provider")
        }
        row("Base URL:") {
            cell(baseUrlField)
                .comment("The API endpoint URL (required for OpenAI and Ollama)")
        }
        row("Model Name:") {
            cell(modelNameField)
                .comment("The name of the model to use")
        }
        row("Display Name:") {
            cell(displayNameField)
                .comment("Optional: A friendly name to show in the UI")
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
        return null
    }

    fun getProvider(): CustomLlmProvider {
        val type: LlmProviderType = providerTypeComboBox.selectedItem as LlmProviderType
        return CustomLlmProvider(
            name = nameField.text,
            displayName = displayNameField.text.takeIf { it.isNotBlank() },
            type = type,
            baseUrl = baseUrlField.text?:"",
            modelName = modelNameField?.text ?:"",
        )
    }
}
