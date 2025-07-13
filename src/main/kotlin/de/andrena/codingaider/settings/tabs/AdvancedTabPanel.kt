package de.andrena.codingaider.settings.tabs

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Panel
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
    }

    override fun isModified(): Boolean {
        return useSidecarModeCheckBox.isSelected != settings.useSidecarMode ||
                sidecarModeVerboseCheckBox.isSelected != settings.sidecarModeVerbose ||
                activateIdeExecutorAfterWebcrawlCheckBox.isSelected != settings.activateIdeExecutorAfterWebcrawl ||
                webCrawlLlmComboBox.selectedItem.asSelectedItemName() != settings.webCrawlLlm ||
                deactivateRepoMapCheckBox.isSelected != settings.deactivateRepoMap ||
                verboseCommandLoggingCheckBox.isSelected != settings.verboseCommandLogging ||
                enableLocalModelCostMapCheckBox.isSelected != settings.enableLocalModelCostMap ||
                mountAiderConfInDockerCheckBox.isSelected != settings.mountAiderConfInDocker ||
                showWorkingDirectoryPanelCheckBox.isSelected != settings.showWorkingDirectoryPanel
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
