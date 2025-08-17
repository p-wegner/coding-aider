package de.andrena.codingaider.services.mcp

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.services.mcp.tools.CreateAiderPlanTool
import de.andrena.codingaider.services.mcp.tools.ManagePersistentFilesTool
import de.andrena.codingaider.settings.AiderSettings

/**
 * Registry service for discovering and managing MCP tools
 */
@Service(Service.Level.PROJECT)
class McpToolRegistry(private val project: Project) {
    
    companion object {
        private val LOG = Logger.getInstance(McpToolRegistry::class.java)
        
        fun getInstance(project: Project): McpToolRegistry = 
            project.service<McpToolRegistry>()
    }
    
    private val tools = mutableMapOf<String, McpTool>()
    private val toolMetadata = mutableMapOf<String, McpToolMetadata>()
    private val toolConfigurations = mutableMapOf<String, Boolean>()
    private val settings by lazy { AiderSettings.getInstance() }
    private val changeListeners = mutableListOf<() -> Unit>()
    
    init {
        discoverTools()
    }
    
    /**
     * Discover and register all available MCP tools
     */
    private fun discoverTools() {
        LOG.info("Discovering MCP tools...")
        
        // Register built-in persistent file management tool
        registerTool(ManagePersistentFilesTool(project))
        
        // Register plan management tools
        registerTool(CreateAiderPlanTool(project))
        
        LOG.info("Discovered ${tools.size} MCP tools: ${tools.keys.joinToString(", ")}")
    }
    
    /**
     * Register a tool with the registry
     */
    fun registerTool(tool: McpTool) {
        val name = tool.getName()
        tools[name] = tool
        toolMetadata[name] = McpToolMetadata(
            name = name,
            description = tool.getDescription(),
            inputSchema = tool.getInputSchema(),
            isEnabledByDefault = tool.isEnabledByDefault()
        )
        // Initialize tool configuration with default enabled state
        if (!toolConfigurations.containsKey(name)) {
            toolConfigurations[name] = tool.isEnabledByDefault()
        }
        LOG.debug("Registered MCP tool: $name")
    }
    
    /**
     * Get all registered tools
     */
    fun getAllTools(): Map<String, McpTool> = tools.toMap()
    
    /**
     * Get all tool metadata
     */
    fun getAllToolMetadata(): Map<String, McpToolMetadata> = toolMetadata.toMap()
    
    /**
     * Get available tools (alias for getAllToolMetadata for compatibility)
     */
    fun getAvailableTools(): List<McpToolMetadata> = toolMetadata.values.toList()
    
    /**
     * Get enabled tools based on current settings
     */
    fun getEnabledTools(): List<McpTool> {
        return tools.filter { (name, _) -> isToolEnabled(name) }.values.toList()
    }
    
    /**
     * Get a specific tool by name
     */
    fun getTool(name: String): McpTool? = tools[name]
    
    /**
     * Get metadata for a specific tool
     */
    fun getToolMetadata(name: String): McpToolMetadata? = toolMetadata[name]
    
    /**
     * Check if a tool is enabled in settings
     */
    fun isToolEnabled(toolName: String): Boolean {
        return toolConfigurations[toolName] ?: (toolMetadata[toolName]?.isEnabledByDefault ?: false)
    }
    
    /**
     * Get the count of registered tools
     */
    fun getToolCount(): Int = tools.size
    
    /**
     * Get the count of enabled tools
     */
    fun getEnabledToolCount(): Int = getEnabledTools().size
    
    /**
     * Update tool configuration
     */
    fun updateToolConfiguration(newToolConfigurations: Map<String, Boolean>) {
        toolConfigurations.putAll(newToolConfigurations)
        LOG.info("Tool configuration updated: $newToolConfigurations")
        LOG.info("Current enabled tools: ${getEnabledTools().map { it.getName() }}")
        notifyChangeListeners()
    }
    
    /**
     * Get current tool configurations
     */
    fun getToolConfigurations(): Map<String, Boolean> = toolConfigurations.toMap()
    
    /**
     * Add a listener for tool registry changes
     */
    fun addToolRegistryChangeListener(listener: () -> Unit) {
        changeListeners.add(listener)
    }
    
    /**
     * Remove a tool registry change listener
     */
    fun removeToolRegistryChangeListener(listener: () -> Unit) {
        changeListeners.remove(listener)
    }
    
    /**
     * Notify all change listeners
     */
    private fun notifyChangeListeners() {
        changeListeners.forEach { it() }
    }
}
