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
    private val settings = AiderSettings.getInstance()
    
    private val statusLabel = JBLabel("Status: Checking...")
    private val portLabel = JBLabel("Port: -")
    private val endpointLabel = JBLabel("Endpoint: -")
    private val startButton = JButton("Start Server")
    private val stopButton = JButton("Stop Server")
    private val toolsTextArea = JTextArea().apply {
        isEditable = false
        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        text = getMcpToolsInfo()
    }
    
    private val refreshTimer = Timer(2000) { updateStatus() }
    
    init {
        setupButtons()
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
    
    private fun updateStatus() {
        val isRunning = mcpServerService.isServerRunning()
        val port = if (isRunning) mcpServerService.getServerPort() else settings.mcpServerPort
        
        statusLabel.text = "Status: ${mcpServerService.getServerStatus()}"
        portLabel.text = "Port: $port"
        endpointLabel.text = if (isRunning) {
            "Endpoint: http://localhost:$port/mcp"
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
    }
    
    private fun getMcpToolsInfo(): String {
        return """
MCP Tools Available:

1. get_persistent_files
   Description: Get the current list of persistent files in the project context
   Parameters: None
   
2. add_persistent_files  
   Description: Add files to the persistent files context
   Parameters:
   - files: Array of file objects with filePath and optional isReadOnly

3. remove_persistent_files
   Description: Remove files from the persistent files context  
   Parameters:
   - filePaths: Array of file paths to remove

4. clear_persistent_files
   Description: Clear all files from the persistent files context
   Parameters: None

Usage:
Connect your MCP client to: http://localhost:${settings.mcpServerPort}/mcp
Health check: http://localhost:${settings.mcpServerPort}/health
Status: http://localhost:${settings.mcpServerPort}/status

The server provides HTTP-based MCP communication for managing
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
            
            group("Available MCP Tools") {
                row {
                    cell(JBScrollPane(toolsTextArea)).align(com.intellij.ui.dsl.builder.AlignX.FILL)
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
