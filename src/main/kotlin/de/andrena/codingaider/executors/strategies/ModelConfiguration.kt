package de.andrena.codingaider.executors.strategies

object ModelConfiguration {
    
    private val REASONING_EFFORT_SUPPORTED_MODELS = listOf(
        // OpenAI models that support reasoning_effort
        "gpt-4.5-preview", "openai/gpt-4.5-preview",
        "o1-preview", "azure/o1", "openrouter/openai/o1", "openai/o1", "o1",
        "azure/o3", "openai/o3", "o3",
        "azure/o3-pro", "openai/o3-pro", "o3-pro",
        "azure/o4-mini", "openai/o4-mini", "openrouter/openai/o4-mini", "o4-mini",
        "gpt-4.1", "azure/gpt-4.1", "openrouter/openai/gpt-4.1",
        "gpt-5", "openai/gpt-5", "azure/gpt-5", "openrouter/openai/gpt-5",
        "gpt-5-mini", "openai/gpt-5-mini", "azure/gpt-5-mini", "openrouter/openai/gpt-5-mini",
        
        // XAI models that support reasoning_effort
        "xai/grok-4", "openrouter/x-ai/grok-4",
        "xai/grok-3-mini-beta", "openrouter/x-ai/grok-3-mini-beta",
        
        // Gemini models that support reasoning_effort
        "gemini/gemini-2.5-flash-preview-04-17", "gemini-2.5-flash-preview-04-17", "vertex_ai/gemini-2.5-flash-preview-04-17",
        "gemini/gemini-2.5-pro-preview-06-05", "vertex_ai/gemini-2.5-pro-preview-06-05",
        "gemini/gemini-2.5-pro", "vertex_ai/gemini-2.5-pro",
        "gemini/gemini-2.5-flash-lite-preview-06-17", "vertex_ai/gemini-2.5-flash",
        "openrouter/google/gemini-2.5-pro-preview-06-05", "openrouter/google/gemini-2.5-pro",
    )
    
    fun supportsReasoningEffort(llm: String): Boolean {
        return REASONING_EFFORT_SUPPORTED_MODELS.any { model -> 
            llm.equals(model, ignoreCase = true) || 
            llm.contains(model, ignoreCase = true) ||
            model.contains(llm, ignoreCase = true)
        }
    }
}