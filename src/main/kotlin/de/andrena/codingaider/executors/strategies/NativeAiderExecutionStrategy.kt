package de.andrena.codingaider.executors.strategies

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.settings.CustomLlmProviderService
import de.andrena.codingaider.settings.LlmProviderType
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.ApiKeyManager

class NativeAiderExecutionStrategy(
    project: Project,
    private val apiKeyChecker: ApiKeyChecker,
    private val settings: AiderSettings
) : AiderExecutionStrategy(project) {

    override fun buildCommand(commandData: CommandData): MutableList<String> {
        return (listOf(settings.aiderExecutablePath) + buildCommonArgs(commandData, settings)).toMutableList()
    }

    override fun prepareEnvironment(processBuilder: ProcessBuilder, commandData: CommandData) {
        setApiKeyEnvironmentVariables(processBuilder, apiKeyChecker, commandData)
    }

    override fun cleanupAfterExecution() {
        // No specific cleanup needed for native execution
    }

    fun setApiKeyEnvironmentVariables(
        processBuilder: ProcessBuilder,
        apiKeyChecker: ApiKeyChecker,
        commandData: CommandData
    ) {
        val environment = processBuilder.environment()

        val customProvider = CustomLlmProviderService.Companion.getInstance().getProvider(commandData.llm)
        when {
            customProvider != null -> {
                // Set provider-specific environment variables
                when (customProvider.type) {
                    LlmProviderType.OPENAI -> {
                        ApiKeyManager.getCustomModelKey(customProvider.name)?.let { apiKey ->
                            environment["OPENAI_API_KEY"] = apiKey
                            if (customProvider.baseUrl.isNotEmpty()) {
                                environment["OPENAI_API_BASE"] = customProvider.baseUrl
                            }
                        }
                    }

                    LlmProviderType.OLLAMA -> {
                        environment["OLLAMA_HOST"] = customProvider.baseUrl
                    }

                    LlmProviderType.OPENROUTER -> {
                        ApiKeyManager.getCustomModelKey(customProvider.name)?.let { apiKey ->
                            environment["OPENROUTER_API_KEY"] = apiKey
                        }
                    }

                    LlmProviderType.VERTEX_EXPERIMENTAL -> {
                        ApiKeyManager.getCustomModelKey(customProvider.name)?.let { credentials ->
                            environment["GOOGLE_APPLICATION_CREDENTIALS"] = credentials
                        }
                        if (customProvider.projectId.isNotEmpty()) {
                            environment["VERTEXAI_PROJECT"] = customProvider.projectId
                        }
                        if (customProvider.location.isNotEmpty()) {
                            environment["VERTEXAI_LOCATION"] = customProvider.location
                        }
                    }

                    LlmProviderType.CUSTOM_AIDERMODEL -> {
                    }

                    LlmProviderType.LMSTUDIO -> TODO()
                }
            }

            else -> {
                // Set standard API keys
                apiKeyChecker.getAllApiKeyNames().forEach { keyName ->
                    apiKeyChecker.getApiKeyValue(keyName)?.let { value ->
                        environment[keyName] = value
                    }
                }

                // Add local model cost mapping if enabled
                if (settings.enableLocalModelCostMap) {
                    environment["LITELLM_LOCAL_MODEL_COST_MAP"] = "True"
                }
            }
        }

    }

}
