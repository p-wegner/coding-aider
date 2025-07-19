package de.andrena.codingaider.settings.tabs

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Panel
import de.andrena.codingaider.settings.AiderDefaults
import de.andrena.codingaider.settings.ExperimentalFeatureUtil
import de.andrena.codingaider.settings.LlmComboBoxRenderer
import de.andrena.codingaider.settings.LlmSelection
import de.andrena.codingaider.utils.ApiKeyChecker
import java.awt.event.ItemEvent
import javax.swing.JComboBox

/**
 * Advanced settings tab panel
 */
class AdvancedTabPanel(apiKeyChecker: ApiKeyChecker) : SettingsTabPanel(apiKeyChecker) {

    // UI Components
    private val useSidecarModeCheckBox = JBCheckBox("Use Sidecar Mode (Experimental)")
    private val sidecarModeVerboseCheckBox = JBCheckBox("Enable verbose logging for sidecar mode")
    private val activateIdeExecutorAfterWebcrawlCheckBox =
        JBCheckBox("Activate Post web crawl LLM cleanup (Experimental)")
    private val webCrawlLlmComboBox: JComboBox<LlmSelection> = ComboBox(apiKeyChecker.getAllLlmOptions().toTypedArray())
    private val deactivateRepoMapCheckBox = JBCheckBox("Deactivate Aider's repo map (--map-tokens 0)")
    private val verboseCommandLoggingCheckBox = JBCheckBox("Enable verbose Aider command logging")
    private val mountAiderConfInDockerCheckBox = JBCheckBox("Mount Aider configuration file in Docker")
    private val enableLocalModelCostMapCheckBox = JBCheckBox("Enable local model cost mapping")
    private val showDevToolsCheckBox = JBCheckBox("Show DevTools button in markdown viewer")
    private val showWorkingDirectoryPanelCheckBox = JBCheckBox("Show working directory panel in tool window")
    
    // MCP Server settings
    private val enableMcpServerCheckBox = JBCheckBox("Enable MCP Server")
    private val mcpServerAutoStartCheckBox = JBCheckBox("Auto-start MCP server with plugin")
    private val mcpServerPortField = com.intellij.ui.components.JBTextField()

    override fun getTabName(): String = "Advanced"

    override fun getTabTooltip(): String = "Advanced settings for Aider"

    override fun createPanel(panel: Panel) {
        panel.apply {
            group("Execution Mode") {
                row {
                    cell(useSidecarModeCheckBox).component.apply {
                        text = "Use Sidecar Mode"
                        ExperimentalFeatureUtil.markAsExperimental(this)
                        toolTipText =
                            "Run Aider as a persistent process. This is experimental and may improve performance."
                        addItemListener { e ->
                            sidecarModeVerboseCheckBox.isEnabled = e.stateChange == ItemEvent.SELECTED
                        }
                    }
                }
                row {
                    cell(sidecarModeVerboseCheckBox).component.apply {
                        toolTipText = "Enable detailed logging for sidecar mode operations"
                        isEnabled = useSidecarModeCheckBox.isSelected
                    }
                }
                row {
                    cell(mountAiderConfInDockerCheckBox).component.apply {
                        toolTipText =
                            "If enabled, the Aider configuration file will be mounted in the Docker container."
                    }
                }
            }

            group("Web Crawl") {
                row {
                    cell(activateIdeExecutorAfterWebcrawlCheckBox)
                        .component
                        .apply {
                            text = "Activate Post web crawl LLM cleanup"
                            ExperimentalFeatureUtil.markAsExperimental(this)
                            toolTipText = "This option prompts Aider to clean up the crawled markdown. " +
                                    "Note that this experimental feature may exceed the LLM's token limit and potentially leads to high costs. " +
                                    "Use it with caution."
                        }
                }
                row("Web Crawl LLM:") {
                    cell(webCrawlLlmComboBox).component.apply {
                        renderer = LlmComboBoxRenderer(apiKeyChecker)
                        toolTipText = "Select the LLM model to use for web crawl operations"
                    }
                }
            }

            group("Performance and Logging") {
                row {
                    cell(deactivateRepoMapCheckBox)
                        .component
                        .apply {
                            toolTipText =
                                "This will deactivate Aider's repo map. Saves time for repo updates, but will give aider less context."
                        }
                }
                row {
                    cell(verboseCommandLoggingCheckBox)
                        .component
                        .apply {
                            toolTipText =
                                "If enabled, Aider command details will be logged in the dialog shown to the user. This may show sensitive information."
                        }
                }
                row {
                    cell(enableLocalModelCostMapCheckBox)
                        .applyToComponent {
                            toolTipText =
                                "When enabled, local model cost mapping will be activated. This will save some http requests on aider startup but may have outdated price information."
                        }
                }
            }

            group("Output Presentation") {
                row {
                    cell(showDevToolsCheckBox)
                        .component
                        .apply {
                            toolTipText =
                                "If enabled, a DevTools button will be shown in the markdown viewer for debugging purposes."
                        }
                }
                row {
                    cell(showWorkingDirectoryPanelCheckBox)
                        .component
                        .apply {
                            toolTipText =
                                "If enabled, the working directory panel will be shown in the tool window."
                        }
                }
            }

            group("MCP Server") {
                row {
                    cell(enableMcpServerCheckBox)
                        .component
                        .apply {
                            toolTipText =
                                "Enable the Model Context Protocol (MCP) server for persistent file management. " +
                                "This allows external MCP clients to interact with the plugin's persistent files."
                            addItemListener { e ->
                                val enabled = e.stateChange == java.awt.event.ItemEvent.SELECTED
                                mcpServerAutoStartCheckBox.isEnabled = enabled
                                mcpServerPortField.isEnabled = enabled
                            }
                        }
                }
                row {
                    cell(mcpServerAutoStartCheckBox)
                        .component
                        .apply {
                            toolTipText =
                                "If enabled, the MCP server will start automatically when the plugin loads."
                        }
                }
                row("Server Port:") {
                    cell(mcpServerPortField)
                        .component
                        .apply {
                            toolTipText =
                                "Port number for the MCP server. Default is 8080. The server will find the next available port if this one is occupied."
                            columns = 10
                        }
                }
            }
        }
    }

    override fun apply() {
        settings.useSidecarMode = useSidecarModeCheckBox.isSelected
        settings.sidecarModeVerbose = sidecarModeVerboseCheckBox.isSelected
        settings.activateIdeExecutorAfterWebcrawl = activateIdeExecutorAfterWebcrawlCheckBox.isSelected
        settings.webCrawlLlm = webCrawlLlmComboBox.selectedItem.asSelectedItemName()
        settings.deactivateRepoMap = deactivateRepoMapCheckBox.isSelected
        settings.verboseCommandLogging = verboseCommandLoggingCheckBox.isSelected
        settings.enableLocalModelCostMap = enableLocalModelCostMapCheckBox.isSelected
        settings.mountAiderConfInDocker = mountAiderConfInDockerCheckBox.isSelected
        settings.showWorkingDirectoryPanel = showWorkingDirectoryPanelCheckBox.isSelected
        settings.showDevTools = showDevToolsCheckBox.isSelected
        
        // MCP Server settings
        settings.enableMcpServer = enableMcpServerCheckBox.isSelected
        settings.mcpServerAutoStart = mcpServerAutoStartCheckBox.isSelected
        try {
            settings.mcpServerPort = mcpServerPortField.text.toIntOrNull() ?: AiderDefaults.MCP_SERVER_PORT
        } catch (e: NumberFormatException) {
            settings.mcpServerPort = AiderDefaults.MCP_SERVER_PORT
        }
    }

    override fun reset() {
        useSidecarModeCheckBox.isSelected = settings.useSidecarMode
        sidecarModeVerboseCheckBox.isSelected = settings.sidecarModeVerbose
        sidecarModeVerboseCheckBox.isEnabled = settings.useSidecarMode
        activateIdeExecutorAfterWebcrawlCheckBox.isSelected = settings.activateIdeExecutorAfterWebcrawl
        webCrawlLlmComboBox.selectedItem = apiKeyChecker.getLlmSelectionForName(settings.webCrawlLlm)
        deactivateRepoMapCheckBox.isSelected = settings.deactivateRepoMap
        verboseCommandLoggingCheckBox.isSelected = settings.verboseCommandLogging
        enableLocalModelCostMapCheckBox.isSelected = settings.enableLocalModelCostMap
        mountAiderConfInDockerCheckBox.isSelected = settings.mountAiderConfInDocker
        showWorkingDirectoryPanelCheckBox.isSelected = settings.showWorkingDirectoryPanel
        showDevToolsCheckBox.isSelected = settings.showDevTools
        
        // MCP Server settings
        enableMcpServerCheckBox.isSelected = settings.enableMcpServer
        mcpServerAutoStartCheckBox.isSelected = settings.mcpServerAutoStart
        mcpServerAutoStartCheckBox.isEnabled = settings.enableMcpServer
        mcpServerPortField.isEnabled = settings.enableMcpServer
        mcpServerPortField.text = settings.mcpServerPort.toString()
    }

    override fun isModified(): Boolean {
        val mcpServerPortModified = try {
            mcpServerPortField.text.toIntOrNull() != settings.mcpServerPort
        } catch (e: NumberFormatException) {
            true
        }
        
        return useSidecarModeCheckBox.isSelected != settings.useSidecarMode ||
                sidecarModeVerboseCheckBox.isSelected != settings.sidecarModeVerbose ||
                activateIdeExecutorAfterWebcrawlCheckBox.isSelected != settings.activateIdeExecutorAfterWebcrawl ||
                webCrawlLlmComboBox.selectedItem.asSelectedItemName() != settings.webCrawlLlm ||
                deactivateRepoMapCheckBox.isSelected != settings.deactivateRepoMap ||
                verboseCommandLoggingCheckBox.isSelected != settings.verboseCommandLogging ||
                enableLocalModelCostMapCheckBox.isSelected != settings.enableLocalModelCostMap ||
                mountAiderConfInDockerCheckBox.isSelected != settings.mountAiderConfInDocker ||
                showWorkingDirectoryPanelCheckBox.isSelected != settings.showWorkingDirectoryPanel ||
                showDevToolsCheckBox.isSelected != settings.showDevTools ||
                enableMcpServerCheckBox.isSelected != settings.enableMcpServer ||
                mcpServerAutoStartCheckBox.isSelected != settings.mcpServerAutoStart ||
                mcpServerPortModified
    }

    fun updateLlmOptions(llmOptions: Array<LlmSelection>) {
        val currentSelection = webCrawlLlmComboBox.selectedItem as? LlmSelection
        webCrawlLlmComboBox.model = javax.swing.DefaultComboBoxModel(llmOptions)
        if (currentSelection != null && llmOptions.contains(currentSelection)) {
            webCrawlLlmComboBox.selectedItem = currentSelection
        }
    }

    private fun Any?.asSelectedItemName(): String {
        val selection = this as? LlmSelection ?: return ""
        return selection.name.ifBlank { "" }
    }
    
    /**
     * Disable sidecar mode (used when Docker mode is enabled)
     */
    fun disableSidecarMode() {
        useSidecarModeCheckBox.isSelected = false
        useSidecarModeCheckBox.isEnabled = false
        sidecarModeVerboseCheckBox.isEnabled = false
    }
    
    /**
     * Enable sidecar mode (used when Docker mode is disabled)
     */
    fun enableSidecarMode() {
        useSidecarModeCheckBox.isEnabled = true
    }
}
