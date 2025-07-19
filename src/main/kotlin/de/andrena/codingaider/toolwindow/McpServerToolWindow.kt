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
    
    // Tool enable/disable checkboxes
    private val getPersistentFilesCheckbox = JCheckBox("get_persistent_files", true)
    private val addPersistentFilesCheckbox = JCheckBox("add_persistent_files", true)
    private val removePersistentFilesCheckbox = JCheckBox("remove_persistent_files", true)
    private val clearPersistentFilesCheckbox = JCheckBox("clear_persistent_files", true)
    
    private val refreshTimer = Timer(2000) { updateStatus() }
    
    init {
        setupButtons()
        setupToolCheckboxes()
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
    
    private fun setupToolCheckboxes() {
        // Add listeners to update server configuration when checkboxes change
        getPersistentFilesCheckbox.addActionListener { updateToolConfiguration() }
        addPersistentFilesCheckbox.addActionListener { updateToolConfiguration() }
        removePersistentFilesCheckbox.addActionListener { updateToolConfiguration() }
        clearPersistentFilesCheckbox.addActionListener { updateToolConfiguration() }
    }
    
    private fun updateToolConfiguration() {
        // Update the MCP server with the new tool configuration
        mcpServerService.updateToolConfiguration(
            enableGetPersistentFiles = getPersistentFilesCheckbox.isSelected,
            enableAddPersistentFiles = addPersistentFilesCheckbox.isSelected,
            enableRemovePersistentFiles = removePersistentFilesCheckbox.isSelected,
            enableClearPersistentFiles = clearPersistentFilesCheckbox.isSelected
        )
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
    }
    
    private fun getConnectionInfo(): String {
        val port = if (mcpServerService.isServerRunning()) mcpServerService.getServerPort() else settings.mcpServerPort
        return """
Connection Information:

MCP Endpoint: http://localhost:$port/sse
Health Check: http://localhost:$port/health
Status: http://localhost:$port/status

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
                row { 
                    cell(getPersistentFilesCheckbox)
                    comment("Get the current list of persistent files")
                }
                row { 
                    cell(addPersistentFilesCheckbox)
                    comment("Add files to the persistent files context")
                }
                row { 
                    cell(removePersistentFilesCheckbox)
                    comment("Remove files from the persistent files context")
                }
                row { 
                    cell(clearPersistentFilesCheckbox)
                    comment("Clear all files from the persistent files context")
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
