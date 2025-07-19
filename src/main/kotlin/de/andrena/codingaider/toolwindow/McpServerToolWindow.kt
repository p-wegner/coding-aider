package de.andrena.codingaider.toolwindow

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.services.McpServerService
import de.andrena.codingaider.settings.AiderSettings
import kotlinx.coroutines.*
import javax.swing.*

class McpServerToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val mcpServerToolWindow = McpServerToolWindow(project)
        val content = ContentFactory.getInstance().createContent(mcpServerToolWindow.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class McpServerToolWindow(private val project: Project) {
    private val mcpServerService = project.service<McpServerService>()
    private val settings = AiderSettings.getInstance()
    
    private val statusLabel = JLabel("Server Status: Stopped")
    private val portLabel = JLabel("Port: N/A")
    private val startStopButton = JButton("Start Server")
    private val toolsListModel = DefaultListModel<String>()
    private val toolsList = JList(toolsListModel)
    
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    init {
        setupToolsList()
        updateUI()
        startStatusMonitoring()
    }
    
    fun getContent(): JComponent {
        return panel {
            group("MCP Server Status") {
                row {
                    cell(statusLabel)
                }
                row {
                    cell(portLabel)
                }
                row {
                    cell(startStopButton).applyToComponent {
                        addActionListener {
                            toggleServer()
                        }
                    }
                }
            }
            
            group("Available MCP Tools") {
                row {
                    scrollCell(toolsList).apply {
                        component.selectionMode = ListSelectionModel.SINGLE_SELECTION
                    }
                }.resizableRow()
            }
        }
    }
    
    private fun setupToolsList() {
        toolsListModel.addElement("get_persistent_files - Get the current list of persistent files")
        toolsListModel.addElement("add_persistent_files - Add files to the persistent files context")
        toolsListModel.addElement("remove_persistent_files - Remove files from the persistent files context")
        toolsListModel.addElement("clear_persistent_files - Clear all files from the persistent files context")
    }
    
    private fun toggleServer() {
        if (mcpServerService.isServerRunning()) {
            mcpServerService.stopServer()
        } else {
            mcpServerService.startServer()
        }
        updateUI()
    }
    
    private fun updateUI() {
        SwingUtilities.invokeLater {
            val isRunning = mcpServerService.isServerRunning()
            statusLabel.text = "Server Status: ${if (isRunning) "Running" else "Stopped"}"
            portLabel.text = "Port: ${if (isRunning) mcpServerService.getServerPort() else "N/A"}"
            startStopButton.text = if (isRunning) "Stop Server" else "Start Server"
            startStopButton.isEnabled = settings.enableMcpServer
        }
    }
    
    private fun startStatusMonitoring() {
        coroutineScope.launch {
            while (true) {
                updateUI()
                delay(2000) // Update every 2 seconds
            }
        }
    }
    
    fun dispose() {
        coroutineScope.cancel()
    }
}
