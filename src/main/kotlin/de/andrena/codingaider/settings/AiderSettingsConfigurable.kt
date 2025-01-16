package de.andrena.codingaider.settings

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.DefaultApiKeyChecker
import java.awt.Component
import javax.swing.*

class AiderSettingsConfigurable() : Configurable {

    private val apiKeyChecker: ApiKeyChecker = service<DefaultApiKeyChecker>()
    private var settingsComponent: JPanel? = null
    private val aiderSetupPanel = AiderSetupPanel(apiKeyChecker)
    private val useYesFlagCheckBox = JBCheckBox("Use --yes flag by default")
    private var llmOptions = apiKeyChecker.getAllLlmOptions().toTypedArray()
    private val llmComboBox: JComboBox<LlmSelection>
    private val customProviderService = CustomLlmProviderService.getInstance()
    private val customProviderListener: () -> Unit = { updateLlmOptions() }
    private val manageProvidersButton = JButton("Manage Providers...").apply {
        addActionListener {
            CustomLlmProviderDialog().show()
        }
    }
    private val additionalArgsField = JBTextField()
    private val lintCmdField = JBTextField()
    private val showGitComparisonToolCheckBox = JBCheckBox("Show git comparison tool after execution")
    private val activateIdeExecutorAfterWebcrawlCheckBox =
        JBCheckBox("Activate Post web crawl LLM cleanup (Experimental)")
    private val webCrawlLlmComboBox = ComboBox(apiKeyChecker.getAllLlmOptions().toTypedArray())
    private val deactivateRepoMapCheckBox = JBCheckBox("Deactivate Aider's repo map (--map-tokens 0)")
    private val editFormatComboBox = ComboBox(arrayOf("", "whole", "diff", "whole-func", "diff-func"))
    private val verboseCommandLoggingCheckBox = JBCheckBox("Enable verbose Aider command logging")
    private val enableMarkdownDialogAutocloseCheckBox = JBCheckBox("Automatically close Output Dialog")
    private val markdownDialogAutocloseDelayField = JBTextField()
    private val mountAiderConfInDockerCheckBox = JBCheckBox("Mount Aider configuration file in Docker")
    private val includeChangeContextCheckBox = JBCheckBox("Include change context in commit messages")
    private val autoCommitsComboBox = ComboBox(arrayOf("Default", "On", "Off"))
    private val dirtyCommitsComboBox = ComboBox(arrayOf("Default", "On", "Off"))
    private val useSidecarModeCheckBox = JBCheckBox("Use Sidecar Mode (Experimental)")
    private val sidecarModeVerboseCheckBox = JBCheckBox("Enable verbose logging for sidecar mode")
    private val enableDocumentationLookupCheckBox = JBCheckBox("Enable documentation lookup")
    private val alwaysIncludeOpenFilesCheckBox = JBCheckBox("Always include open files in context")
    private val alwaysIncludePlanContextFilesCheckBox = JBCheckBox("Always include plan context files")
    private val enableAutoPlanContinueCheckBox = JBCheckBox("Enable automatic plan continuation")
    private val documentationLlmComboBox =
        ComboBox(apiKeyChecker.getAllLlmOptions().toTypedArray())
    private val summarizedOutputCheckBox = JBCheckBox("Enable summarized output")

    override fun getDisplayName(): String = "Aider"

    override fun createComponent(): JComponent {
        settingsComponent = panel {
            aiderSetupPanel.createPanel(this)

            group("General Settings") {
                row { cell(useYesFlagCheckBox) }
                row("Default LLM Model:") {
                    cell(llmComboBox).component.apply {
                        renderer = LlmComboBoxRenderer()
                    }
                    cell(manageProvidersButton)
                }
                row("Default Additional Arguments:") {
                    cell(additionalArgsField)
                        .resizableColumn()
                        .align(Align.FILL)
                    link("Aider options documentation") {
                        BrowserUtil.browse("https://aider.chat/docs/config/options.html")
                    }
                }
                row { cell(alwaysIncludeOpenFilesCheckBox) }
            }

            group("Code Modification Settings") {
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
                            toolTipText =
                                "Select the default edit format for Aider. Leave empty to use the default format for the used LLM."
                        }
                }
            }

            group("Git Settings") {
                row { cell(showGitComparisonToolCheckBox) }
                row("Auto-commits:") {
                    cell(autoCommitsComboBox)
                        .component
                        .apply {
                            toolTipText =
                                "Default: Use system setting. On: Aider will automatically commit changes after each successful edit. Off: Disable auto-commits."
                        }
                }
                row("Dirty-commits:") {
                    cell(dirtyCommitsComboBox)
                        .component
                        .apply {
                            toolTipText =
                                "Default: Use system setting. On: Aider will allow commits even when there are uncommitted changes in the repo. Off: Disable dirty-commits."
                        }
                }
                row {
                    cell(includeChangeContextCheckBox)
                        .component
                        .apply {
                            toolTipText =
                                "If enabled, the commit messages will include the user prompt and affected files."
                        }
                }

            }

            group("Advanced Settings") {
                row {
                    cell(useSidecarModeCheckBox).component.apply {
                        toolTipText =
                            "Run Aider as a persistent process. This is experimental and may improve performance."
                        addItemListener { e ->
                            sidecarModeVerboseCheckBox.isEnabled = e.stateChange == java.awt.event.ItemEvent.SELECTED
                        }
                        addActionListener {
                            if (isSelected && useDockerAiderCheckBox.isSelected) {
                                isSelected = false
                                Messages.showWarningDialog(
                                    "Sidecar mode cannot be used with Docker. Please disable Docker first.",
                                    "Incompatible Settings"
                                )
                            }
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
                    cell(activateIdeExecutorAfterWebcrawlCheckBox)
                        .component
                        .apply {
                            toolTipText = "This option prompts Aider to clean up the crawled markdown. " +
                                    "Note that this experimental feature may exceed the LLM's token limit and potentially leads to high costs. " +
                                    "Use it with caution."
                        }
                    cell(webCrawlLlmComboBox)
                        .label("Web Crawl LLM:")
                }
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
                    cell(enableMarkdownDialogAutocloseCheckBox)
                        .component
                        .apply {
                            toolTipText =
                                "If enabled, the Output Dialog will automatically close after the specified delay."
                            addItemListener { e ->
                                markdownDialogAutocloseDelayField.isEnabled =
                                    e.stateChange == java.awt.event.ItemEvent.SELECTED
                            }
                        }
                    label("Autoclose delay (seconds):")
                    cell(markdownDialogAutocloseDelayField)
                        .component
                        .apply {
                            toolTipText =
                                "Specify the delay in seconds before the Output Dialog closes automatically. Set to 0 for immediate closing."
                            isEnabled = enableMarkdownDialogAutocloseCheckBox.isSelected
                        }
                }
                row {
                    cell(mountAiderConfInDockerCheckBox)
                        .component
                        .apply {
                            toolTipText =
                                "If enabled, the Aider configuration file will be mounted in the Docker container."
                        }
                }
                row {
                    cell(enableDocumentationLookupCheckBox).component.apply {
                        toolTipText =
                            "If enabled, documentation files (*.md) in parent directories will be included in the context"
                    }
                }
                row { cell(alwaysIncludePlanContextFilesCheckBox) }
                row {
                    cell(enableAutoPlanContinueCheckBox).component.apply {
                        toolTipText =
                            "If enabled, plans will automatically continue when there are open checklist items"
                    }
                }
                row("Documentation LLM Model:") {
                    cell(documentationLlmComboBox).component.apply {
                        renderer = LlmComboBoxRenderer()
                        toolTipText =
                            "Select the LLM model to use for generating documentation. The default is the LLM model specified in the settings."
                    }
                }
                row {
                    cell(summarizedOutputCheckBox)
                        .applyToComponent {
                            toolTipText =
                                "When enabled, Aider will include XML-tagged summaries of changes in its output"
                        }
                }

            }

        }.apply {
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }

        return settingsComponent!!
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
                documentationLlmComboBox.selectedItem.asSelectedItemName() != settings.documentationLlm ||
                summarizedOutputCheckBox.isSelected != settings.summarizedOutput ||
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
        settings.documentationLlm = documentationLlmComboBox.selectedItem.asSelectedItemName()
        settings.summarizedOutput = summarizedOutputCheckBox.isSelected
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
        documentationLlmComboBox.selectedItem = apiKeyChecker.getLlmSelectionForName(settings.documentationLlm)
        summarizedOutputCheckBox.isSelected = settings.summarizedOutput
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
