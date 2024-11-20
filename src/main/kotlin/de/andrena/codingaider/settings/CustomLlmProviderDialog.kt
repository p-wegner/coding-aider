package de.andrena.codingaider.settings

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent
import javax.swing.JPanel

class CustomLlmProviderDialog(
    private val project: Project,
    private val existingProvider: CustomLlmProvider? = null
) : DialogWrapper(project) {

    private val nameField = JBTextField()
    private val baseUrlField = JBTextField()
    private val modelNameField = JBTextField()
    private val displayNameField = JBTextField()
    private val providerTypeComboBox = com.intellij.openapi.ui.ComboBox(ProviderType.values())

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
        val selectedType = providerTypeComboBox.selectedItem as ProviderType
        if (selectedType.requiresBaseUrl && baseUrlField.text.isBlank()) {
            return ValidationInfo("Base URL is required for ${selectedType.name}", baseUrlField)
        }
        return null
    }

    fun getProvider(): CustomLlmProvider {
        return CustomLlmProvider(
            name = nameField.text,
            type = providerTypeComboBox.selectedItem as ProviderType,
            baseUrl = baseUrlField.text.takeIf { it.isNotBlank() },
            modelName = modelNameField.text,
            displayName = displayNameField.text.takeIf { it.isNotBlank() }
        )
    }
}
