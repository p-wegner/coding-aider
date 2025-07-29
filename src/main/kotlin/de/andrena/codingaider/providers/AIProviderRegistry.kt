package de.andrena.codingaider.providers

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

/**
 * Registry for managing AI providers and their execution strategies
 */
@Service(Service.Level.PROJECT)
class AIProviderRegistry(private val project: Project) {
    
    private val executionStrategies = mutableMapOf<Pair<AIProvider, String>, AIExecutionStrategy>()
    private val outputParsers = mutableMapOf<AIProvider, OutputParser>()
    private val processInteractors = mutableMapOf<AIProvider, AIProcessInteractor>()
    private val processManagers = mutableMapOf<AIProvider, AIProcessManager>()
    
    /**
     * Registers an execution strategy for a specific provider and strategy type
     * @param provider The AI provider
     * @param strategyType The strategy type (e.g., "native", "docker", "sidecar")
     * @param strategy The execution strategy implementation
     */
    fun registerExecutionStrategy(provider: AIProvider, strategyType: String, strategy: AIExecutionStrategy) {
        executionStrategies[Pair(provider, strategyType)] = strategy
    }
    
    /**
     * Registers an output parser for a specific provider
     * @param provider The AI provider
     * @param parser The output parser implementation
     */
    fun registerOutputParser(provider: AIProvider, parser: OutputParser) {
        outputParsers[provider] = parser
    }
    
    /**
     * Registers a process interactor for a specific provider
     * @param provider The AI provider
     * @param interactor The process interactor implementation
     */
    fun registerProcessInteractor(provider: AIProvider, interactor: AIProcessInteractor) {
        processInteractors[provider] = interactor
    }
    
    /**
     * Registers a process manager for a specific provider
     * @param provider The AI provider
     * @param manager The process manager implementation
     */
    fun registerProcessManager(provider: AIProvider, manager: AIProcessManager) {
        processManagers[provider] = manager
    }
    
    /**
     * Gets the execution strategy for a provider and strategy type
     * @param provider The AI provider
     * @param strategyType The strategy type
     * @return The execution strategy, or null if not found
     */
    fun getExecutionStrategy(provider: AIProvider, strategyType: String): AIExecutionStrategy? {
        return executionStrategies[Pair(provider, strategyType)]
    }
    
    /**
     * Gets the output parser for a provider
     * @param provider The AI provider
     * @return The output parser, or null if not found
     */
    fun getOutputParser(provider: AIProvider): OutputParser? {
        return outputParsers[provider]
    }
    
    /**
     * Gets the process interactor for a provider
     * @param provider The AI provider
     * @return The process interactor, or null if not found
     */
    fun getProcessInteractor(provider: AIProvider): AIProcessInteractor? {
        return processInteractors[provider]
    }
    
    /**
     * Gets the process manager for a provider
     * @param provider The AI provider
     * @return The process manager, or null if not found
     */
    fun getProcessManager(provider: AIProvider): AIProcessManager? {
        return processManagers[provider]
    }
    
    /**
     * Gets all available providers that have at least one execution strategy registered
     * @return List of available AI providers
     */
    fun getAvailableProviders(): List<AIProvider> {
        return executionStrategies.keys.map { it.first }.distinct()
    }
    
    /**
     * Gets all available strategy types for a specific provider
     * @param provider The AI provider
     * @return List of strategy types for the provider
     */
    fun getAvailableStrategyTypes(provider: AIProvider): List<String> {
        return executionStrategies.keys.filter { it.first == provider }.map { it.second }
    }
    
    /**
     * Checks if a provider and strategy combination is available
     * @param provider The AI provider
     * @param strategyType The strategy type
     * @return true if the combination is available, false otherwise
     */
    fun isProviderStrategyAvailable(provider: AIProvider, strategyType: String): Boolean {
        val strategy = getExecutionStrategy(provider, strategyType)
        return strategy?.isProviderAvailable() == true
    }
}