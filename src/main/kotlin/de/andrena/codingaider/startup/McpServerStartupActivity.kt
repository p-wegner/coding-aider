package de.andrena.codingaider.startup

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import de.andrena.codingaider.services.McpServerService

@Service(Service.Level.PROJECT)
class McpServerStartupActivity : ProjectActivity {
    
    companion object {
        private val LOG = Logger.getInstance(McpServerStartupActivity::class.java)
    }
    
    override suspend fun execute(project: Project) {
        try {
            LOG.info("Initializing MCP server service for project: ${project.name}")
            
            // Get the service instance to trigger initialization
            val mcpServerService = project.service<McpServerService>()
            
            LOG.info("MCP server service initialized successfully. Status: ${mcpServerService.getServerStatus()}")
            
            // Log connection information for users
            if (mcpServerService.isServerRunning()) {
                val port = mcpServerService.getServerPort()
                LOG.info("MCP server is ready for client connections at http://localhost:$port/mcp")
            }
            
        } catch (e: Exception) {
            LOG.error("Failed to initialize MCP server service for project: ${project.name}", e)
        }
    }
}