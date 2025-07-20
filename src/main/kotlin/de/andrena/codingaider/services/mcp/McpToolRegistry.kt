package de.andrena.codingaider.services.mcp

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.services.mcp.tools.AddPersistentFilesTool
import de.andrena.codingaider.services.mcp.tools.ClearPersistentFilesTool
import de.andrena.codingaider.services.mcp.tools.GetPersistentFilesTool
import de.andrena.codingaider.services.mcp.tools.RemovePersistentFilesTool
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
    private val settings by lazy { AiderSettings.getInstance() }
    
    init {
        discoverTools()
    }
    
    /**
     * Discover and register all available MCP tools
     */
    private fun discoverTools() {
        LOG.info("Discovering MCP tools...")
        
        // Register built-in persistent file tools
        registerTool(GetPersistentFilesTool(project))
        registerTool(AddPersistentFilesTool(project))
        registerTool(RemovePersistentFilesTool(project))
        registerTool(ClearPersistentFilesTool(project))
        
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
        // For now, use the existing individual settings
        // This will be refactored when we update the settings system
        return when (toolName) {
            "get_persistent_files" -> true // Always enabled for now
            "add_persistent_files" -> true
            "remove_persistent_files" -> true
            "clear_persistent_files" -> true
            else -> toolMetadata[toolName]?.isEnabledByDefault ?: false
        }
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
     * Update tool configuration (for compatibility with McpServerService)
     */
    fun updateToolConfiguration(toolConfigurations: Map<String, Boolean>) {
        // For now, this is a placeholder
        // In a full implementation, this would update the settings
        LOG.info("Tool configuration updated: $toolConfigurations")
    }
}
