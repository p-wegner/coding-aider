package de.andrena.codingaider.toolwindow.mcpserver

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.services.McpServerService
import de.andrena.codingaider.settings.AiderSettings
import java.awt.Color
import java.awt.Font
import javax.swing.JComponent
import javax.swing.Timer

class McpServerPanel(private val project: Project) {
    private val mcpServerService = project.service<McpServerService>()
    private val settings = AiderSettings.getInstance()
    
    private val statusLabel = JBLabel("Status: Unknown").apply {
        font = font.deriveFont(Font.BOLD)
    }
    private val portField = JBTextField().apply {
        text = settings.mcpServerPort.toString()
        columns = 6
    }
    private val toolsLabel = JBLabel("Active Tools: Loading...")
    
    // Timer to refresh status periodically
    private val refreshTimer = Timer(2000) { updateStatus() }
    
    init {
        updateStatus()
        refreshTimer.start()
    }

    fun getContent(): JComponent {
        return panel {
            row {
                val toolbar = ActionManager.getInstance().createActionToolbar(
                    "McpServerToolbar",
                    DefaultActionGroup().apply {
                        add(object : AnAction(
                            "Start MCP Server", 
                            "Start the MCP server",
                            AllIcons.Actions.Execute
                        ) {
                            override fun actionPerformed(e: AnActionEvent) {
                                val port = portField.text.toIntOrNull() ?: settings.mcpServerPort
                                mcpServerService.startServer(port)
                                updateStatus()
                            }
                            
                            override fun getActionUpdateThread() = ActionUpdateThread.BGT
                            
                            override fun update(e: AnActionEvent) {
                                e.presentation.isEnabled = !mcpServerService.isServerRunning()
                            }
                        })
                        add(object : AnAction(
                            "Stop MCP Server", 
                            "Stop the MCP server",
                            AllIcons.Actions.Suspend
                        ) {
                            override fun actionPerformed(e: AnActionEvent) {
                                mcpServerService.stopServer()
                                updateStatus()
                            }
                            
                            override fun getActionUpdateThread() = ActionUpdateThread.BGT
                            
                            override fun update(e: AnActionEvent) {
                                e.presentation.isEnabled = mcpServerService.isServerRunning()
                            }
                        })
                        add(object : AnAction(
                            "Refresh Status", 
                            "Refresh server status",
                            AllIcons.Actions.Refresh
                        ) {
                            override fun actionPerformed(e: AnActionEvent) {
                                updateStatus()
                            }
                        })
                    },
                    true
                )
                cell(Wrapper(toolbar.component))
            }
            
            row("Port:") {
                cell(portField).apply {
                    component.toolTipText = "Port number for the MCP server"
                }
            }
            
            row {
                cell(statusLabel)
            }
            
            row {
                cell(toolsLabel)
            }
            
            row {
                text("MCP Server provides tools for external clients to interact with persistent files.")
                    .applyToComponent {
                        font = font.deriveFont(Font.ITALIC)
                        foreground = Color.GRAY
                    }
            }
        }
    }
    
    private fun updateStatus() {
        val isRunning = mcpServerService.isServerRunning()
        val port = mcpServerService.getServerPort()
        
        if (isRunning) {
            statusLabel.text = "Status: Running on port $port"
            statusLabel.foreground = Color.GREEN.darker()
            toolsLabel.text = "Active Tools: list_persistent_files, add_persistent_files, remove_persistent_files, get_persistent_file_content"
        } else {
            statusLabel.text = "Status: Stopped"
            statusLabel.foreground = Color.RED.darker()
            toolsLabel.text = "Active Tools: None"
        }
        
        // Update port field if it differs from current server port
        if (portField.text.toIntOrNull() != port) {
            portField.text = port.toString()
        }
    }
    
    fun dispose() {
        refreshTimer.stop()
    }
}
