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
import de.andrena.codingaider.utils.EnvironmentVariableManager
import javax.swing.JComponent
import javax.swing.JLabel
import com.intellij.util.ui.JBUI
import java.awt.Color

class CustomLlmProviderEditorDialog(
    private val existingProvider: CustomLlmProvider? = null
) : DialogWrapper(null) {

    private val nameField = JBTextField()
    private val baseUrlField = JBTextField()
    private val modelNameField = JBTextField()
    private val apiKeyField = JBPasswordField()
    private val projectIdField = JBTextField()
    private val locationField = JBTextField()
    private val customEnvVarField = JBTextField()
    private lateinit var baseUrlRow: Row
    private lateinit var apiKeyRow: Row
    private lateinit var customEnvVarRow: Row
    private lateinit var projectIdRow: Row
    private lateinit var locationRow: Row
    private val providerTypeComboBox = com.intellij.openapi.ui.ComboBox(LlmProviderType.values())
    private var apiKeyStatusLabel = JLabel()

    init {
        title = if (existingProvider == null) "Add Custom LLM Provider" else "Edit Custom LLM Provider"
        init()

        // Add document listeners to trigger validation on input changes
        nameField.document.addDocumentListener(createValidationListener())
        baseUrlField.document.addDocumentListener(createValidationListener())
        modelNameField.document.addDocumentListener(createValidationListener())
        apiKeyField.document.addDocumentListener(createValidationListener())
        customEnvVarField.document.addDocumentListener(createValidationListener())

        providerTypeComboBox.addActionListener {
            updateProviderTypeUI()
        }

        // Set initial values
        if (existingProvider != null) {
            nameField.text = existingProvider.name
            baseUrlField.text = existingProvider.baseUrl
            modelNameField.text = existingProvider.modelName
            providerTypeComboBox.selectedItem = existingProvider.type
            projectIdField.text = existingProvider.projectId
            locationField.text = existingProvider.location
            customEnvVarField.text = existingProvider.customEnvironmentVariable

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
        
        // Update provider type tooltip with authentication info
        val authInfo = when {
            !selectedType.supportsApiKey -> "No authentication required"
            selectedType.apiKeyOptional -> "API key optional - falls back to environment variables: ${selectedType.getDefaultEnvironmentVariables().joinToString(", ")}"
            else -> "API key required - set ${selectedType.getApiKeyName()} environment variable or provide explicit key"
        }
        providerTypeComboBox.toolTipText = "${selectedType.displayName}: $authInfo"

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
                apiKeyField.toolTipText = "OpenRouter API key (optional - will fall back to OPENROUTER_API_KEY environment variable)"
            }

            LlmProviderType.OPENAI -> {
                if (existingProvider == null) {
                    baseUrlField.text = "https://api.openai.com/v1"
                }
                baseUrlField.isEnabled = true
                baseUrlField.toolTipText = "OpenAI API base URL"
                apiKeyField.isEnabled = true
                apiKeyField.toolTipText = "OpenAI API key (optional - will fall back to OPENAI_API_KEY environment variable)"
            }

            LlmProviderType.VERTEX_EXPERIMENTAL -> {
                baseUrlField.isEnabled = false
                apiKeyField.isEnabled = false
                apiKeyField.text = ""
                projectIdField.isEnabled = true
                locationField.isEnabled = true
            }
            LlmProviderType.LMSTUDIO -> {
                if (existingProvider == null) {
                    baseUrlField.text = "http://127.0.0.1:1234"
                }
                baseUrlField.isEnabled = true
                baseUrlField.toolTipText = "LM Studio API base URL"
                apiKeyField.isEnabled = true
                apiKeyField.toolTipText = "LM Studio API key (optional - will fall back to LM_STUDIO_API_KEY or OPENAI_API_KEY environment variables)"
            }

            else -> {
                baseUrlField.isEnabled = true
                baseUrlField.toolTipText = "API endpoint URL"
                apiKeyField.isEnabled = selectedType.supportsApiKey
                apiKeyField.toolTipText = if (selectedType.apiKeyOptional) {
                    "API key (optional - will fall back to environment variables: ${selectedType.getDefaultEnvironmentVariables().joinToString(", ")})"
                } else {
                    "API key (required)"
                }
                projectIdField.isEnabled = false
                locationField.isEnabled = false
            }
        }

        // Update row visibility and tooltips based on provider requirements
        baseUrlRow.visible(selectedType.requiresBaseUrl)
        apiKeyRow.visible(selectedType.supportsApiKey)
        customEnvVarRow.visible(selectedType.supportsApiKey && selectedType.apiKeyOptional)
        projectIdRow.visible(selectedType == LlmProviderType.VERTEX_EXPERIMENTAL)
        locationRow.visible(selectedType == LlmProviderType.VERTEX_EXPERIMENTAL)

        // Update model name tooltip and validation based on provider type
        modelNameField.toolTipText = when (selectedType) {
            LlmProviderType.VERTEX_EXPERIMENTAL -> "Model name without vertex_ai/ prefix. ${selectedType.exampleModels}"
            else -> selectedType.exampleModels
        }
        
        // Update API key status
        updateApiKeyStatus()
    }
    
    private fun updateApiKeyStatus() {
        val selectedType = providerTypeComboBox.selectedItem as LlmProviderType
        
        if (!selectedType.supportsApiKey) {
            apiKeyStatusLabel.text = "🔓 No authentication required"
            apiKeyStatusLabel.foreground = JBUI.CurrentTheme.Label.foreground()
            apiKeyStatusLabel.toolTipText = "This provider type does not require authentication"
            return
        }
        
        val envManager = service<EnvironmentVariableManager>()
        val hasApiKey = String(apiKeyField.password).isNotBlank() && String(apiKeyField.password) != "*".repeat(16)
        val customEnvVar = customEnvVarField.text.trim()
        
        when {
            hasApiKey -> {
                apiKeyStatusLabel.text = "🔑 API key provided"
                apiKeyStatusLabel.foreground = Color(0, 128, 0) // Green
                apiKeyStatusLabel.toolTipText = "API key is explicitly provided and will be used for authentication"
            }
            selectedType.apiKeyOptional -> {
                val hasCustomEnvVar = customEnvVar.isNotBlank() && envManager.isEnvironmentVariableAvailable(customEnvVar)
                val hasDefaultEnvVars = envManager.areAnyEnvironmentVariablesAvailable(selectedType.getDefaultEnvironmentVariables())
                
                when {
                    hasCustomEnvVar -> {
                        apiKeyStatusLabel.text = "🌍 Custom environment variable: $customEnvVar"
                        apiKeyStatusLabel.foreground = Color(0, 128, 0) // Green
                        apiKeyStatusLabel.toolTipText = "Custom environment variable '$customEnvVar' is available and will be used for authentication"
                    }
                    hasDefaultEnvVars -> {
                        val availableVars = selectedType.getDefaultEnvironmentVariables().filter { 
                            envManager.isEnvironmentVariableAvailable(it) 
                        }
                        apiKeyStatusLabel.text = "🌍 Environment variables: ${availableVars.joinToString(", ")}"
                        apiKeyStatusLabel.foreground = Color(0, 128, 0) // Green
                        apiKeyStatusLabel.toolTipText = "Default environment variables are available: ${availableVars.joinToString(", ")}"
                    }
                    else -> {
                        val expectedVars = if (customEnvVar.isNotBlank()) {
                            listOf(customEnvVar) + selectedType.getDefaultEnvironmentVariables()
                        } else {
                            selectedType.getDefaultEnvironmentVariables()
                        }
                        apiKeyStatusLabel.text = "⚠️ No authentication available"
                        apiKeyStatusLabel.foreground = Color(255, 140, 0) // Orange
                        apiKeyStatusLabel.toolTipText = "No API key provided and no environment variables found. Expected: ${expectedVars.joinToString(", ")}"
                    }
                }
            }
            else -> {
                apiKeyStatusLabel.text = "❌ API key required"
                apiKeyStatusLabel.foreground = Color(255, 0, 0) // Red
                apiKeyStatusLabel.toolTipText = "This provider requires an API key. Please provide one or set the ${selectedType.getApiKeyName()} environment variable"
            }
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
                .comment("Select the type of LLM provider. Different types have different authentication requirements.")
        }
        baseUrlRow = row("Base URL:") {
            cell(baseUrlField)
                .columns(30)
                .comment("The API endpoint URL")
        }
        row("Model Name:") {
            cell(modelNameField)
                .columns(30)
                .comment("The name of the model to use")
                .comment((providerTypeComboBox.selectedItem as LlmProviderType).exampleModels)
        }
        apiKeyRow = row("API Key:") {
            cell(apiKeyField)
                .columns(30)
                .comment("Secure API key for the provider")
            cell(apiKeyStatusLabel)
        }
        customEnvVarRow = row("Custom Environment Variable:") {
            cell(customEnvVarField)
                .columns(30)
                .comment("Optional custom environment variable name for API key")
        }
        projectIdRow = row("Project ID:") {
            cell(projectIdField)
                .columns(30)
                .comment("Google Cloud project ID for Vertex AI")
        }
        locationRow = row("Location:") {
            cell(locationField)
                .columns(30)
                .comment("Google Cloud region for Vertex AI (e.g. us-east5)")
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

        if (selectedType.supportsApiKey && !selectedType.apiKeyOptional) {
            val apiKeyText = String(apiKeyField.password)
            // For new providers or when a new key is being set
            if (existingProvider == null || apiKeyText != "*".repeat(16)) {
                if (apiKeyText.isBlank()) {
                    return ValidationInfo("API key is required for ${selectedType.name}", apiKeyField)
                }
            }
        }

        // For optional API keys, validate that either API key or environment variables are available
        if (selectedType.supportsApiKey && selectedType.apiKeyOptional) {
            val apiKeyText = String(apiKeyField.password)
            val hasApiKey = apiKeyText.isNotBlank() && apiKeyText != "*".repeat(16)
            
            if (!hasApiKey) {
                val envManager = service<EnvironmentVariableManager>()
                val customEnvVar = customEnvVarField.text.trim()
                val hasCustomEnvVar = customEnvVar.isNotBlank() && envManager.isEnvironmentVariableAvailable(customEnvVar)
                val hasDefaultEnvVars = envManager.areAnyEnvironmentVariablesAvailable(selectedType.getDefaultEnvironmentVariables())
                
                if (!hasCustomEnvVar && !hasDefaultEnvVars) {
                    val envVarOptions = if (customEnvVar.isNotBlank()) {
                        "$customEnvVar or ${selectedType.getDefaultEnvironmentVariables().joinToString(", ")}"
                    } else {
                        selectedType.getDefaultEnvironmentVariables().joinToString(", ")
                    }
                    return ValidationInfo(
                        "Either provide an API key or ensure environment variables ($envVarOptions) are set",
                        apiKeyField
                    )
                }
            }
        }

        // Validate Vertex AI specific fields
        if (selectedType == LlmProviderType.VERTEX_EXPERIMENTAL) {
            if (projectIdField.text.trim().isEmpty()) {
                return ValidationInfo("Project ID is required for Vertex AI", projectIdField)
            }
            if (!projectIdField.text.trim().matches(Regex("^[a-z][a-z0-9-]*[a-z0-9]$"))) {
                return ValidationInfo(
                    "Invalid project ID format. Must start with letter, contain only lowercase letters, numbers, and hyphens",
                    projectIdField
                )
            }
            if (locationField.text.trim().isEmpty()) {
                return ValidationInfo("Location is required for Vertex AI", locationField)
            }
            if (!locationField.text.trim().matches(Regex("^[a-z]+-[a-z]+\\d+$"))) {
                return ValidationInfo("Invalid location format. Example: us-central1", locationField)
            }
            // Validate model name format for Vertex AI
            val modelName = modelNameField.text.trim()
            if (!modelName.matches(Regex("^[\\w-]+(@latest|@\\d{8})?$"))) {
                return ValidationInfo(
                    "Invalid model name format for Vertex AI. Examples: claude-3-sonnet@latest, gemini-pro@20240620",
                    modelNameField
                )
            }
            // Ensure model name doesn't already include the vertex_ai/ prefix
            if (modelName.startsWith("vertex_ai/")) {
                return ValidationInfo(
                    "Model name should not include the vertex_ai/ prefix - it will be added automatically",
                    modelNameField
                )
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
            updateApiKeyStatus()  // Update API key status when fields change
        }
    }

    fun getProvider(): CustomLlmProvider {
        val type: LlmProviderType = providerTypeComboBox.selectedItem as LlmProviderType
        val apiKeyText = String(apiKeyField.password)
        val hasExplicitApiKey = apiKeyText.isNotBlank() && apiKeyText != "*".repeat(16)
        
        val provider = CustomLlmProvider(
            name = nameField.text,
            type = type,
            baseUrl = baseUrlField.text,
            modelName = modelNameField.text,
            projectId = projectIdField.text.trim(),
            location = locationField.text.trim(),
            hasExplicitApiKey = hasExplicitApiKey,
            customEnvironmentVariable = customEnvVarField.text.trim()
        )

        // Save API key if provided and not masked, except for Ollama and Vertex AI
        if (type.supportsApiKey && type != LlmProviderType.VERTEX_EXPERIMENTAL) {
            if (hasExplicitApiKey) {
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
