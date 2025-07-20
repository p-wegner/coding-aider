package de.andrena.codingaider.toolwindow

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.services.McpServerService
import de.andrena.codingaider.services.mcp.McpToolRegistry
import de.andrena.codingaider.settings.AiderSettings
import java.awt.Font
import javax.swing.*

class McpServerToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val mcpServerToolWindow = McpServerToolWindow(project)
        val content = ContentFactory.getInstance().createContent(
            mcpServerToolWindow.getContent(),
            "",
            false
        )
        toolWindow.contentManager.addContent(content)
    }
}

class McpServerToolWindow(private val project: Project) {
    
    private val mcpServerService = project.service<McpServerService>()
    private val mcpToolRegistry = McpToolRegistry.getInstance(project)
    private val settings = AiderSettings.getInstance()
    
    private val statusLabel = JBLabel("Status: Checking...")
    private val portLabel = JBLabel("Port: -")
    private val endpointLabel = JBLabel("Endpoint: -")
    private val startButton = JButton("Start Server")
    private val stopButton = JButton("Stop Server")
    
    // Dynamic tool checkboxes
    private val toolCheckboxes = mutableMapOf<String, JCheckBox>()
    
    private val refreshTimer = Timer(2000) { updateStatus() }
    
    init {
        setupButtons()
        setupDynamicToolCheckboxes()
        updateStatus()
        refreshTimer.start()
    }
    
    private fun setupButtons() {
        startButton.addActionListener {
            mcpServerService.startServer()
            updateStatus()
        }
        
        stopButton.addActionListener {
            mcpServerService.stopServer()
            updateStatus()
        }
    }
    
    private fun setupDynamicToolCheckboxes() {
        // Create checkboxes for all available tools
        val availableTools = mcpToolRegistry.getAvailableTools()
        val toolConfigurations = mcpToolRegistry.getToolConfigurations()
        
        toolCheckboxes.clear()
        availableTools.forEach { metadata ->
            val isEnabled = toolConfigurations[metadata.name] ?: metadata.isEnabledByDefault
            val checkbox = JCheckBox(metadata.name, isEnabled)
            checkbox.addActionListener { updateToolConfiguration() }
            toolCheckboxes[metadata.name] = checkbox
        }
    }
    
    private fun updateToolConfiguration() {
        // Update the MCP server with the new tool configuration
        val toolConfigurations = toolCheckboxes.mapValues { (_, checkbox) -> checkbox.isSelected }
        mcpServerService.updateToolConfiguration(toolConfigurations)
    }
    
    private fun updateStatus() {
        val isRunning = mcpServerService.isServerRunning()
        val port = if (isRunning) mcpServerService.getServerPort() else settings.mcpServerPort
        
        statusLabel.text = "Status: ${mcpServerService.getServerStatus()}"
        portLabel.text = "Port: $port"
        endpointLabel.text = if (isRunning) {
            "Endpoint: http://localhost:$port/sse"
        } else {
            "Endpoint: Not running"
        }
        
        startButton.isEnabled = !isRunning && settings.enableMcpServer
        stopButton.isEnabled = isRunning
        
        // Update button text based on settings
        if (!settings.enableMcpServer) {
            startButton.text = "Start Server (Disabled in Settings)"
        } else {
            startButton.text = "Start Server"
        }
        
        // Update checkbox states to reflect current configuration
        val toolConfigurations = mcpToolRegistry.getToolConfigurations()
        toolCheckboxes.forEach { (toolName, checkbox) ->
            val isEnabled = toolConfigurations[toolName] ?: mcpToolRegistry.getToolMetadata(toolName)?.isEnabledByDefault ?: false
            if (checkbox.isSelected != isEnabled) {
                checkbox.isSelected = isEnabled
            }
        }
    }
    
    private fun getConnectionInfo(): String {
        val port = if (mcpServerService.isServerRunning()) mcpServerService.getServerPort() else settings.mcpServerPort
        val enabledToolCount = mcpToolRegistry.getEnabledTools().size
        val totalToolCount = mcpToolRegistry.getAvailableTools().size
        
        return """
Connection Information:

MCP Endpoint: http://localhost:$port/sse
Health Check: http://localhost:$port/health
Status: http://localhost:$port/status

Tools: $enabledToolCount/$totalToolCount enabled

The server provides SSE-based MCP communication for managing
persistent files in the Coding-Aider plugin context.
        """.trimIndent()
    }
    
    fun getContent(): JComponent {
        return panel {
            group("MCP Server Status") {
                row { cell(statusLabel) }
                row { cell(portLabel) }
                row { cell(endpointLabel) }
                row {
                    cell(startButton)
                    cell(stopButton)
                }
            }
            
            group("MCP Tools Configuration") {
                val availableTools = mcpToolRegistry.getAvailableTools()
                availableTools.forEach { metadata ->
                    val checkbox = toolCheckboxes[metadata.name]
                    if (checkbox != null) {
                        row { 
                            cell(checkbox)
                            comment(metadata.description)
                        }
                    }
                }
                
                if (availableTools.isEmpty()) {
                    row {
                        label("No MCP tools available")
                    }
                }
            }
            
            group("Connection Information") {
                row {
                    val connectionTextArea = JTextArea().apply {
                        isEditable = false
                        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
                        text = getConnectionInfo()
                        background = null
                        border = null
                    }
                    cell(JBScrollPane(connectionTextArea)).align(com.intellij.ui.dsl.builder.AlignX.FILL)
                        .resizableColumn()
                }.resizableRow()
            }
        }.apply {
            preferredSize = java.awt.Dimension(400, 600)
        }
    }
    
    fun dispose() {
        refreshTimer.stop()
    }
}
