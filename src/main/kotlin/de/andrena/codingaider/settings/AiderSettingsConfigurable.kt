package de.andrena.codingaider.settings

import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.tabs.TabInfo
import com.intellij.ui.tabs.impl.JBTabsImpl
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.DefaultApiKeyChecker
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.*

class AiderSettingsConfigurable() : Configurable {

    private val apiKeyChecker: ApiKeyChecker = service<DefaultApiKeyChecker>()
    private var settingsComponent: JPanel? = null
    private val tabsComponent = JBTabsImpl(null, null)
    
    // Setup panel
    private val aiderSetupPanel = AiderSetupPanel(apiKeyChecker) { useDockerAider ->
        if (useDockerAider) {
            useSidecarModeCheckBox.isSelected = false
        }
        useSidecarModeCheckBox.isEnabled = !useDockerAider
    }
    
    // General settings
    private val useYesFlagCheckBox = JBCheckBox("Use --yes flag by default")
    private var llmOptions = apiKeyChecker.getAllLlmOptions().toTypedArray()
    private val llmComboBox: JComboBox<LlmSelection>
    private val additionalArgsField = JBTextField()
    private val alwaysIncludeOpenFilesCheckBox = JBCheckBox("Always include open files in context")
    private val defaultModeComboBox = ComboBox(AiderMode.values())
    
    // Code modification settings
    private val lintCmdField = JBTextField()
    private val editFormatComboBox = ComboBox(arrayOf("", "whole", "diff", "udiff", "diff-fenced"))
    private val promptAugmentationCheckBox = JBCheckBox("Enable prompt augmentation")
    private val includeCommitMessageBlockCheckBox = JBCheckBox("Include commit message block")
    private val reasoningEffortComboBox = ComboBox(arrayOf("", "low", "medium", "high"))
    
    // Plugin-based edits settings
    private val pluginBasedEditsCheckBox = JBCheckBox("Use Plugin-Based Edits (Experimental)")
    private val lenientEditsCheckBox = JBCheckBox("Allow Lenient Edits (Process multiple formats) (Experimental)")
    private val autoCommitAfterEditsCheckBox = JBCheckBox("Auto-commit after plugin-based edits (Experimental)")
    
    // Git settings
    private val showGitComparisonToolCheckBox = JBCheckBox("Show git comparison tool after execution")
    private val includeChangeContextCheckBox = JBCheckBox("Include change context in commit messages")
    private val autoCommitsComboBox = ComboBox(arrayOf("Default", "On", "Off"))
    private val dirtyCommitsComboBox = ComboBox(arrayOf("Default", "On", "Off"))
    
    // Plan settings
    private val alwaysIncludePlanContextFilesCheckBox = JBCheckBox("Always include plan context files")
    private val enableAutoPlanContinueCheckBox = JBCheckBox("Enable automatic plan continuation")
    private val enableSubplansCheckBox = JBCheckBox("Enable subplans for complex features")
    private val enableDocumentationLookupCheckBox = JBCheckBox("Enable documentation lookup")
    private val documentationLlmComboBox = ComboBox(apiKeyChecker.getAllLlmOptions().toTypedArray())
    
    // Advanced settings
    private val useSidecarModeCheckBox = JBCheckBox("Use Sidecar Mode (Experimental)")
    private val sidecarModeVerboseCheckBox = JBCheckBox("Enable verbose logging for sidecar mode")
    private val activateIdeExecutorAfterWebcrawlCheckBox = JBCheckBox("Activate Post web crawl LLM cleanup (Experimental)")
    private val webCrawlLlmComboBox = ComboBox(apiKeyChecker.getAllLlmOptions().toTypedArray())
    private val deactivateRepoMapCheckBox = JBCheckBox("Deactivate Aider's repo map (--map-tokens 0)")
    private val verboseCommandLoggingCheckBox = JBCheckBox("Enable verbose Aider command logging")
    private val enableMarkdownDialogAutocloseCheckBox = JBCheckBox("Automatically close Output Dialog")
    private val markdownDialogAutocloseDelayField = JBTextField()
    private val mountAiderConfInDockerCheckBox = JBCheckBox("Mount Aider configuration file in Docker")
    private val enableLocalModelCostMapCheckBox = JBCheckBox("Enable local model cost mapping")
    
    private val customProviderService = CustomLlmProviderService.getInstance()
    private val customProviderListener: () -> Unit = { updateLlmOptions() }

    override fun getDisplayName(): String = "Aider"

    override fun createComponent(): JComponent {
        val mainPanel = JPanel(BorderLayout())
        
        // Create tabs
        val setupTab = createSetupTab()
        val generalTab = createGeneralTab()
        val codeModificationTab = createCodeModificationTab()
        val gitTab = createGitTab()
        val planTab = createPlanTab()
        val advancedTab = createAdvancedTab()
        
        // Add tabs to the tabbed pane
        tabsComponent.addTab(setupTab)
        tabsComponent.addTab(generalTab)
        tabsComponent.addTab(codeModificationTab)
        tabsComponent.addTab(gitTab)
        tabsComponent.addTab(planTab)
        tabsComponent.addTab(advancedTab)
        
        mainPanel.add(tabsComponent.component, BorderLayout.CENTER)
        mainPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        
        settingsComponent = mainPanel
        return settingsComponent!!
    }
    
    private fun createSetupTab(): TabInfo {
        val setupPanel = panel {
            aiderSetupPanel.createPanel(this)
        }
        
        return TabInfo(setupPanel).apply {
            text = "Setup"
            tooltip = "Basic Aider setup and API configuration"
        }
    }
    
    private fun createGeneralTab(): TabInfo {
        val generalPanel = panel {
            group("Basic Settings") {
                row { cell(useYesFlagCheckBox).applyToComponent { 
                    toolTipText = "When enabled, Aider will automatically accept changes without asking for confirmation"
                }}
                row("Default Mode:") {
                    cell(defaultModeComboBox).component.apply {
                        toolTipText = "Select the default mode for Aider dialogs"
                    }
                }
                row { cell(alwaysIncludeOpenFilesCheckBox).applyToComponent {
                    toolTipText = "When enabled, all currently open files will be included in the context"
                }}
            }
            
            group("LLM Configuration") {
                row("Default LLM Model:") {
                    cell(llmComboBox).component.apply {
                        renderer = LlmComboBoxRenderer()
                        toolTipText = "Select the default LLM model to use for Aider operations"
                    }
                }
                row("Default Additional Arguments:") {
                    cell(additionalArgsField)
                        .resizableColumn()
                        .align(Align.FILL)
                        .applyToComponent {
                            toolTipText = "Additional command-line arguments to pass to Aider"
                        }
                    link("Aider options documentation") {
                        BrowserUtil.browse("https://aider.chat/docs/config/options.html")
                    }
                }
            }
        }
        
        return TabInfo(generalPanel).apply {
            text = "General"
            tooltip = "Basic configuration options for Aider"
        }
    }
    
    private fun createCodeModificationTab(): TabInfo {
        val codeModificationPanel = panel {
            group("Code Editing") {
                row("Lint Command:") {
                    cell(lintCmdField)
                        .resizableColumn()
                        .align(Align.FILL)
                        .apply {
                            component.toolTipText = "The lint command will be executed after every code change by Aider"
                        }
                }
                row("Edit Format:") {
                    cell(editFormatComboBox)
                        .component
                        .apply {
                            toolTipText = "Select the default edit format for Aider. Leave empty to use the default format for the used LLM."
                        }
                }
                row("Reasoning Effort:") {
                    cell(reasoningEffortComboBox)
                        .component
                        .apply {
                            toolTipText = "Set the default reasoning effort level for the LLM"
                        }
                }
            }
            
            group("Prompt Augmentation") {
                row {
                    cell(promptAugmentationCheckBox)
                        .applyToComponent {
                            toolTipText = "When enabled, Aider will include XML-tagged blocks in the prompt to structure the output"
                            addItemListener { e ->
                                val isSelected = e.stateChange == java.awt.event.ItemEvent.SELECTED
                                includeCommitMessageBlockCheckBox.isEnabled = isSelected

                                // If prompt augmentation is disabled but auto-commit is enabled, show warning
                                if (!isSelected && autoCommitAfterEditsCheckBox.isSelected && pluginBasedEditsCheckBox.isSelected) {
                                    showNotification(
                                        "Warning: Auto-commit requires prompt augmentation with commit message block",
                                        com.intellij.notification.NotificationType.WARNING
                                    )
                                    // Disable auto-commit if prompt augmentation is disabled
                                    autoCommitAfterEditsCheckBox.isSelected = false
                                }
                            }
                        }
                }
                row {
                    cell(includeCommitMessageBlockCheckBox)
                        .applyToComponent {
                            toolTipText = "When enabled, Aider will include an XML block for commit messages in the prompt"
                            isEnabled = promptAugmentationCheckBox.isSelected
                            addItemListener { e ->
                                val isSelected = e.stateChange == java.awt.event.ItemEvent.SELECTED

                                // If commit message block is disabled but auto-commit is enabled, show warning
                                if (!isSelected && autoCommitAfterEditsCheckBox.isSelected && pluginBasedEditsCheckBox.isSelected) {
                                    showNotification(
                                        "Warning: Auto-commit requires prompt augmentation with commit message block",
                                        com.intellij.notification.NotificationType.WARNING
                                    )
                                    // Disable auto-commit if commit message block is disabled
                                    autoCommitAfterEditsCheckBox.isSelected = false
                                }
                            }
                        }
                }
            }

            group("Plugin-Based Edits") {
                row {
                    cell(pluginBasedEditsCheckBox)
                        .component
                        .apply {
                            toolTipText = "If enabled, the plugin handles applying edits using /ask and a specific diff format, bypassing Aider's internal edit formats."
                            addItemListener { e ->
                                val isSelected = e.stateChange == java.awt.event.ItemEvent.SELECTED
                                autoCommitAfterEditsCheckBox.isEnabled = isSelected
                                lenientEditsCheckBox.isEnabled = isSelected
                                
                                // Update commit message block checkbox state based on auto-commit
                                if (isSelected && autoCommitAfterEditsCheckBox.isSelected) {
                                    promptAugmentationCheckBox.isSelected = true
                                    includeCommitMessageBlockCheckBox.isSelected = true
                                }
                            }
                        }
                }
                row {
                    cell(lenientEditsCheckBox)
                        .component
                        .apply {
                            toolTipText = "If enabled, the plugin will process all edit formats (diff, whole, udiff) in a single response, regardless of the configured edit format."
                            isEnabled = pluginBasedEditsCheckBox.isSelected
                        }
                }
                row {
                    cell(autoCommitAfterEditsCheckBox)
                        .component
                        .apply {
                            toolTipText = "If enabled, changes made by plugin-based edits will be automatically committed to Git with a message extracted from the LLM response."
                            isEnabled = pluginBasedEditsCheckBox.isSelected
                            addItemListener { e ->
                                val isSelected = e.stateChange == java.awt.event.ItemEvent.SELECTED
                                if (isSelected && pluginBasedEditsCheckBox.isSelected) {
                                    // Auto-enable prompt augmentation and commit message block when auto-commit is enabled
                                    promptAugmentationCheckBox.isSelected = true
                                    includeCommitMessageBlockCheckBox.isSelected = true
                                }
                            }
                        }
                }
            }
        }
        
        return TabInfo(codeModificationPanel).apply {
            text = "Code Modification"
            tooltip = "Settings for code editing and modification"
        }
    }
    
    private fun createGitTab(): TabInfo {
        val gitPanel = panel {
            group("Git Integration") {
                row { 
                    cell(showGitComparisonToolCheckBox).applyToComponent {
                        toolTipText = "When enabled, the Git comparison tool will be shown after Aider makes changes"
                    }
                }
                row("Auto-commits:") {
                    cell(autoCommitsComboBox)
                        .component
                        .apply {
                            toolTipText = "Default: Use system setting. On: Aider will automatically commit changes after each successful edit. Off: Disable auto-commits."
                        }
                }
                row("Dirty-commits:") {
                    cell(dirtyCommitsComboBox)
                        .component
                        .apply {
                            toolTipText = "Default: Use system setting. On: Aider will allow commits even when there are uncommitted changes in the repo. Off: Disable dirty-commits."
                        }
                }
                row {
                    cell(includeChangeContextCheckBox)
                        .component
                        .apply {
                            toolTipText = "If enabled, the commit messages will include the user prompt and affected files."
                        }
                }
            }
        }
        
        return TabInfo(gitPanel).apply {
            text = "Git"
            tooltip = "Git integration settings"
        }
    }
    
    private fun createPlanTab(): TabInfo {
        val planPanel = panel {
            group("Plan Settings") {
                row { 
                    cell(alwaysIncludePlanContextFilesCheckBox).applyToComponent {
                        toolTipText = "When enabled, files listed in the plan context will always be included"
                    }
                }
                row {
                    cell(enableAutoPlanContinueCheckBox).applyToComponent {
                        toolTipText = "If enabled, plans will automatically continue when there are open checklist items"
                    }
                }
                row {
                    cell(enableSubplansCheckBox).applyToComponent {
                        toolTipText = "If enabled, complex features will be broken down into subplans. Disable for simpler, single-file plans."
                    }
                }
            }
            
            group("Documentation") {
                row {
                    cell(enableDocumentationLookupCheckBox).applyToComponent {
                        toolTipText = "If enabled, documentation files (*.md) in parent directories will be included in the context"
                    }
                }
                row("Documentation LLM Model:") {
                    cell(documentationLlmComboBox).component.apply {
                        renderer = LlmComboBoxRenderer()
                        toolTipText = "Select the LLM model to use for generating documentation. The default is the LLM model specified in the settings."
                    }
                }
            }
        }
        
        return TabInfo(planPanel).apply {
            text = "Plans & Docs"
            tooltip = "Settings for plans and documentation"
        }
    }
    
    private fun createAdvancedTab(): TabInfo {
        val advancedPanel = panel {
            group("Execution Mode") {
                row {
                    cell(useSidecarModeCheckBox).component.apply {
                        toolTipText = "Run Aider as a persistent process. This is experimental and may improve performance."
                        addItemListener { e ->
                            sidecarModeVerboseCheckBox.isEnabled = e.stateChange == java.awt.event.ItemEvent.SELECTED
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
                        toolTipText = "If enabled, the Aider configuration file will be mounted in the Docker container."
                    }
                }
            }
            
            group("Web Crawl") {
                row {
                    cell(activateIdeExecutorAfterWebcrawlCheckBox)
                        .component
                        .apply {
                            toolTipText = "This option prompts Aider to clean up the crawled markdown. " +
                                    "Note that this experimental feature may exceed the LLM's token limit and potentially leads to high costs. " +
                                    "Use it with caution."
                        }
                }
                row("Web Crawl LLM:") {
                    cell(webCrawlLlmComboBox).component.apply {
                        renderer = LlmComboBoxRenderer()
                        toolTipText = "Select the LLM model to use for web crawl operations"
                    }
                }
            }
            
            group("Performance & Logging") {
                row {
                    cell(deactivateRepoMapCheckBox)
                        .component
                        .apply {
                            toolTipText = "This will deactivate Aider's repo map. Saves time for repo updates, but will give aider less context."
                        }
                }
                row {
                    cell(verboseCommandLoggingCheckBox)
                        .component
                        .apply {
                            toolTipText = "If enabled, Aider command details will be logged in the dialog shown to the user. This may show sensitive information."
                        }
                }
                row {
                    cell(enableLocalModelCostMapCheckBox)
                        .applyToComponent {
                            toolTipText = "When enabled, local model cost mapping will be activated. This will save some http requests on aider startup but may have outdated price information."
                        }
                }
            }
            
            group("Output Dialog") {
                row {
                    cell(enableMarkdownDialogAutocloseCheckBox)
                        .component
                        .apply {
                            toolTipText = "If enabled, the Output Dialog will automatically close after the specified delay."
                            addItemListener { e ->
                                markdownDialogAutocloseDelayField.isEnabled =
                                    e.stateChange == java.awt.event.ItemEvent.SELECTED
                            }
                        }
                }
                row("Autoclose delay (seconds):") {
                    cell(markdownDialogAutocloseDelayField)
                        .component
                        .apply {
                            toolTipText = "Specify the delay in seconds before the Output Dialog closes automatically. Set to 0 for immediate closing."
                            isEnabled = enableMarkdownDialogAutocloseCheckBox.isSelected
                        }
                }
            }
        }
        
        return TabInfo(advancedPanel).apply {
            text = "Advanced"
            tooltip = "Advanced settings for Aider"
        }
    }

    override fun isModified(): Boolean {
        val settings = AiderSettings.getInstance()
        return useYesFlagCheckBox.isSelected != settings.useYesFlag ||
                llmComboBox.selectedItem.asSelectedItemName() != settings.llm ||
                additionalArgsField.text != settings.additionalArgs ||
                lintCmdField.text != settings.lintCmd ||
                showGitComparisonToolCheckBox.isSelected != settings.showGitComparisonTool ||
                activateIdeExecutorAfterWebcrawlCheckBox.isSelected != settings.activateIdeExecutorAfterWebcrawl ||
                webCrawlLlmComboBox.selectedItem.asSelectedItemName() != settings.webCrawlLlm ||
                deactivateRepoMapCheckBox.isSelected != settings.deactivateRepoMap ||
                editFormatComboBox.selectedItem as String != settings.editFormat ||
                verboseCommandLoggingCheckBox.isSelected != settings.verboseCommandLogging ||
                enableMarkdownDialogAutocloseCheckBox.isSelected != settings.enableMarkdownDialogAutoclose ||
                markdownDialogAutocloseDelayField.text.toIntOrNull() != settings.markdownDialogAutocloseDelay ||
                mountAiderConfInDockerCheckBox.isSelected != settings.mountAiderConfInDocker ||
                includeChangeContextCheckBox.isSelected != settings.includeChangeContext ||
                autoCommitsComboBox.selectedIndex != settings.autoCommits.toIndex() ||
                dirtyCommitsComboBox.selectedIndex != settings.dirtyCommits.toIndex() ||
                useSidecarModeCheckBox.isSelected != settings.useSidecarMode ||
                sidecarModeVerboseCheckBox.isSelected != settings.sidecarModeVerbose ||
                enableDocumentationLookupCheckBox.isSelected != settings.enableDocumentationLookup ||
                alwaysIncludeOpenFilesCheckBox.isSelected != settings.alwaysIncludeOpenFiles ||
                alwaysIncludePlanContextFilesCheckBox.isSelected != settings.alwaysIncludePlanContextFiles ||
                enableAutoPlanContinueCheckBox.isSelected != settings.enableAutoPlanContinue ||
                enableSubplansCheckBox.isSelected != settings.enableSubplans ||
                documentationLlmComboBox.selectedItem.asSelectedItemName() != settings.documentationLlm ||
                promptAugmentationCheckBox.isSelected != settings.promptAugmentation ||
                includeCommitMessageBlockCheckBox.isSelected != settings.includeCommitMessageBlock ||
                enableLocalModelCostMapCheckBox.isSelected != settings.enableLocalModelCostMap ||
                reasoningEffortComboBox.selectedItem as String != settings.reasoningEffort ||
                defaultModeComboBox.selectedItem != settings.defaultMode ||
                pluginBasedEditsCheckBox.isSelected != settings.pluginBasedEdits || 
                lenientEditsCheckBox.isSelected != settings.lenientEdits ||
                autoCommitAfterEditsCheckBox.isSelected != settings.autoCommitAfterEdits ||
                aiderSetupPanel.isModified()
    }

    private fun Any?.asSelectedItemName(): String {
        val selection = this as LlmSelection
        return selection.name.ifBlank { "" }
    }

    override fun apply() {
        val settings = AiderSettings.getInstance()
        settings.useYesFlag = useYesFlagCheckBox.isSelected
        settings.llm = llmComboBox.selectedItem.asSelectedItemName()
        settings.additionalArgs = additionalArgsField.text
        settings.lintCmd = lintCmdField.text
        settings.showGitComparisonTool = showGitComparisonToolCheckBox.isSelected
        settings.activateIdeExecutorAfterWebcrawl = activateIdeExecutorAfterWebcrawlCheckBox.isSelected
        settings.webCrawlLlm = webCrawlLlmComboBox.selectedItem.asSelectedItemName()
        settings.deactivateRepoMap = deactivateRepoMapCheckBox.isSelected
        settings.editFormat = editFormatComboBox.selectedItem as String
        settings.verboseCommandLogging = verboseCommandLoggingCheckBox.isSelected
        settings.enableMarkdownDialogAutoclose = enableMarkdownDialogAutocloseCheckBox.isSelected
        settings.markdownDialogAutocloseDelay =
            markdownDialogAutocloseDelayField.text.toIntOrNull() ?: AiderDefaults.MARKDOWN_DIALOG_AUTOCLOSE_DELAY_IN_S
        settings.mountAiderConfInDocker = mountAiderConfInDockerCheckBox.isSelected
        settings.includeChangeContext = includeChangeContextCheckBox.isSelected
        settings.autoCommits = AiderSettings.AutoCommitSetting.fromIndex(autoCommitsComboBox.selectedIndex)
        settings.dirtyCommits = AiderSettings.DirtyCommitSetting.fromIndex(dirtyCommitsComboBox.selectedIndex)
        settings.useSidecarMode = useSidecarModeCheckBox.isSelected
        settings.sidecarModeVerbose = sidecarModeVerboseCheckBox.isSelected
        settings.enableDocumentationLookup = enableDocumentationLookupCheckBox.isSelected
        settings.alwaysIncludeOpenFiles = alwaysIncludeOpenFilesCheckBox.isSelected
        settings.alwaysIncludePlanContextFiles = alwaysIncludePlanContextFilesCheckBox.isSelected
        settings.enableAutoPlanContinue = enableAutoPlanContinueCheckBox.isSelected
        settings.enableSubplans = enableSubplansCheckBox.isSelected
        settings.documentationLlm = documentationLlmComboBox.selectedItem.asSelectedItemName()
        settings.promptAugmentation = promptAugmentationCheckBox.isSelected
        settings.includeCommitMessageBlock = includeCommitMessageBlockCheckBox.isSelected
        settings.enableLocalModelCostMap = enableLocalModelCostMapCheckBox.isSelected
        settings.reasoningEffort = reasoningEffortComboBox.selectedItem as String
        settings.defaultMode = defaultModeComboBox.selectedItem as AiderMode
        settings.pluginBasedEdits = pluginBasedEditsCheckBox.isSelected
        settings.lenientEdits = lenientEditsCheckBox.isSelected
        settings.autoCommitAfterEdits = autoCommitAfterEditsCheckBox.isSelected
        aiderSetupPanel.apply()
        settings.notifySettingsChanged()
    }

    override fun reset() {
        val settings = AiderSettings.getInstance()
        val apiKeyChecker = service<DefaultApiKeyChecker>()
        useYesFlagCheckBox.isSelected = settings.useYesFlag
        llmComboBox.selectedItem = apiKeyChecker.getLlmSelectionForName(settings.llm)
        additionalArgsField.text = settings.additionalArgs
        lintCmdField.text = settings.lintCmd
        showGitComparisonToolCheckBox.isSelected = settings.showGitComparisonTool
        activateIdeExecutorAfterWebcrawlCheckBox.isSelected = settings.activateIdeExecutorAfterWebcrawl
        webCrawlLlmComboBox.selectedItem = apiKeyChecker.getLlmSelectionForName(settings.webCrawlLlm)
        deactivateRepoMapCheckBox.isSelected = settings.deactivateRepoMap
        editFormatComboBox.selectedItem = settings.editFormat
        verboseCommandLoggingCheckBox.isSelected = settings.verboseCommandLogging
        enableMarkdownDialogAutocloseCheckBox.isSelected = settings.enableMarkdownDialogAutoclose
        markdownDialogAutocloseDelayField.text = settings.markdownDialogAutocloseDelay.toString()
        mountAiderConfInDockerCheckBox.isSelected = settings.mountAiderConfInDocker
        includeChangeContextCheckBox.isSelected = settings.includeChangeContext
        autoCommitsComboBox.selectedIndex = settings.autoCommits.toIndex()
        dirtyCommitsComboBox.selectedIndex = settings.dirtyCommits.toIndex()
        useSidecarModeCheckBox.isSelected = settings.useSidecarMode && !settings.useDockerAider
        sidecarModeVerboseCheckBox.isSelected = settings.sidecarModeVerbose
        sidecarModeVerboseCheckBox.isEnabled = settings.useSidecarMode && !settings.useDockerAider
        useSidecarModeCheckBox.isEnabled = !settings.useDockerAider
        enableDocumentationLookupCheckBox.isSelected = settings.enableDocumentationLookup
        alwaysIncludeOpenFilesCheckBox.isSelected = settings.alwaysIncludeOpenFiles
        alwaysIncludePlanContextFilesCheckBox.isSelected = settings.alwaysIncludePlanContextFiles
        enableAutoPlanContinueCheckBox.isSelected = settings.enableAutoPlanContinue
        enableSubplansCheckBox.isSelected = settings.enableSubplans
        documentationLlmComboBox.selectedItem = apiKeyChecker.getLlmSelectionForName(settings.documentationLlm)
        promptAugmentationCheckBox.isSelected = settings.promptAugmentation
        includeCommitMessageBlockCheckBox.isSelected = settings.includeCommitMessageBlock
        includeCommitMessageBlockCheckBox.isEnabled = settings.promptAugmentation
        enableLocalModelCostMapCheckBox.isSelected = settings.enableLocalModelCostMap
        reasoningEffortComboBox.selectedItem = settings.reasoningEffort
        defaultModeComboBox.selectedItem = settings.defaultMode
        pluginBasedEditsCheckBox.isSelected = settings.pluginBasedEdits
        lenientEditsCheckBox.isSelected = settings.lenientEdits
        lenientEditsCheckBox.isEnabled = settings.pluginBasedEdits
        autoCommitAfterEditsCheckBox.isSelected = settings.autoCommitAfterEdits
        autoCommitAfterEditsCheckBox.isEnabled = settings.pluginBasedEdits
        aiderSetupPanel.reset()
        settings.notifySettingsChanged()
    }

    private inner class LlmComboBoxRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (component is JLabel && value is LlmSelection) {
                text = value.getDisplayText().ifBlank { "" }

                when {
                    value.provider != null -> {
//                        icon = when (value.provider.type) {
//                            LlmProviderType.OPENAI -> MyIcons.OpenAi
//                            LlmProviderType.OLLAMA -> MyIcons.Ollama
//                            LlmProviderType.OPENROUTER -> MyIcons.OpenRouter
//                        }
                        toolTipText = "Custom ${value.provider.type.displayName} provider: ${value.provider.name}"
                    }

                    !value.isBuiltIn -> {
                        val apiKey = apiKeyChecker.getApiKeyForLlm(value.name)
                        if (apiKey != null && !apiKeyChecker.isApiKeyAvailableForLlm(value.name)) {
                            icon = UIManager.getIcon("OptionPane.errorIcon")
                            toolTipText = "API key not found in default locations for ${value.name}"
                        } else {
                            icon = null
                            toolTipText = null
                        }
                    }
                }
            }
            return component
        }
    }

    private fun updateApiKeyFieldsOnClose() {
        aiderSetupPanel.updateApiKeyFieldsOnClose()
    }

    override fun disposeUIResources() {
        updateApiKeyFieldsOnClose()
        customProviderService.removeSettingsChangeListener(customProviderListener)
        settingsComponent = null
        // Ensure that any pending changes are saved
        if (isModified) {
            apply()
        }
    }

    private fun updateLlmOptions() {
        llmOptions = apiKeyChecker.getAllLlmOptions().toTypedArray()
        updateOptions(llmComboBox)
        updateOptions(webCrawlLlmComboBox)
        updateOptions(documentationLlmComboBox)
    }

    private fun updateOptions(llmSelectionWidget: JComboBox<LlmSelection>) {
        val currentSelection = llmSelectionWidget.selectedItem as? LlmSelection
        llmSelectionWidget.model = DefaultComboBoxModel(llmOptions)
        if (currentSelection != null && llmOptions.contains(currentSelection)) {
            llmSelectionWidget.selectedItem = currentSelection
        }
    }
    
    private fun showNotification(content: String, type: com.intellij.notification.NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Coding Aider Notifications")
            .createNotification(content, type)
            .notify(null)
    }

    init {
        this.llmComboBox = llmComboBox(llmOptions)
        customProviderService.addSettingsChangeListener(customProviderListener)
    }

    private fun llmComboBox(llmOptions: Array<LlmSelection>): JComboBox<LlmSelection> =
        object : JComboBox<LlmSelection>(llmOptions) {
            override fun getToolTipText(): String? {
                val selectedItem = selectedItem as? String ?: return null
                return if (apiKeyChecker.isApiKeyAvailableForLlm(selectedItem)) {
                    "API key found for $selectedItem"
                } else {
                    "API key not found for $selectedItem"
                }
            }
        }
}
