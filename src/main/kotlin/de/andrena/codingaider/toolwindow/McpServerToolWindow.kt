package de.andrena.codingaider.toolwindow

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.services.McpServerService
import de.andrena.codingaider.services.mcp.McpToolRegistry
import de.andrena.codingaider.settings.AiderSettings
import java.awt.Font
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.table.DefaultTableModel

class McpServerToolWindowFactory : ToolWindowFactory {
    override fun shouldBeAvailable(project: Project): Boolean {
        val settings = AiderSettings.getInstance()
        return settings.enableMcpServer
    }
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val mcpServerToolWindow = McpServerToolWindow(project)
        
        // Connect the tool window to the MCP server service for logging
        val mcpServerService = project.service<McpServerService>()
        mcpServerService.setToolWindow(mcpServerToolWindow)
        
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
    
    // Tool execution log
    private val toolLogModel = DefaultTableModel(
        arrayOf("Timestamp", "Tool", "Status", "Message"),
        0
    )
    private val toolLogTable = JTable(toolLogModel)
    
    private val refreshTimer = Timer(2000) { updateStatus() }
    
    init {
        setupButtons()
        setupDynamicToolCheckboxes()
        setupToolLogTable()
        updateStatus()
        refreshTimer.start()
        
        // Listen for tool registry changes
        mcpToolRegistry.addToolRegistryChangeListener { 
            // Update connection info when tools change, but don't call updateToolConfiguration
            // to avoid circular calls
            SwingUtilities.invokeLater { updateConnectionInfo() }
        }
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
            checkbox.addActionListener { 
                updateToolConfiguration()
            }
            toolCheckboxes[metadata.name] = checkbox
        }
    }
    
    private fun setupToolLogTable() {
        toolLogTable.apply {
            autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN
            columnModel.getColumn(0).preferredWidth = 120 // Timestamp
            columnModel.getColumn(1).preferredWidth = 150 // Tool
            columnModel.getColumn(2).preferredWidth = 80  // Status
            columnModel.getColumn(3).preferredWidth = 300 // Message
        }
    }
    
    private fun updateToolConfiguration() {
        // Update the MCP server with the new tool configuration
        val toolConfigurations = toolCheckboxes.mapValues { (_, checkbox) -> checkbox.isSelected }
        mcpServerService.updateToolConfiguration(toolConfigurations)
        
        // Log the configuration change
        addToolLogEntry("SYSTEM", "SUCCESS", "Tool configuration updated")
    }
    
    fun addToolLogEntry(toolName: String, status: String, message: String) {
        SwingUtilities.invokeLater {
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            toolLogModel.addRow(arrayOf(timestamp, toolName, status, message))
            
            // Keep only last 100 entries
            while (toolLogModel.rowCount > 100) {
                toolLogModel.removeRow(0)
            }
            
            // Scroll to bottom
            val lastRow = toolLogModel.rowCount - 1
            if (lastRow >= 0) {
                toolLogTable.scrollRectToVisible(toolLogTable.getCellRect(lastRow, 0, true))
            }
        }
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
    
    private var connectionInfoTextArea: JTextArea? = null
    
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
    
    private fun updateConnectionInfo() {
        connectionInfoTextArea?.text = getConnectionInfo()
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
                    connectionInfoTextArea = JTextArea().apply {
                        isEditable = false
                        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
                        text = getConnectionInfo()
                        background = null
                        border = null
                    }
                    cell(JBScrollPane(connectionInfoTextArea!!)).align(com.intellij.ui.dsl.builder.AlignX.FILL)
                        .resizableColumn()
                }.resizableRow()
            }
            
            group("Tool Execution Log") {
                row {
                    cell(JBScrollPane(toolLogTable)).align(com.intellij.ui.dsl.builder.AlignX.FILL)
                        .resizableColumn()
                }.resizableRow()
                row {
                    button("Clear Log") {
                        toolLogModel.rowCount = 0
                    }
                }
            }
        }.apply {
            preferredSize = java.awt.Dimension(400, 600)
        }
    }
    
    fun dispose() {
        refreshTimer.stop()
    }
}
