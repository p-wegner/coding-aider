package de.andrena.codingaider.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import de.andrena.codingaider.inputdialog.AiderMode

@Service(Service.Level.APP)
@State(
    name = "de.andrena.codingaider.settings.AiderSettings",
    storages = [Storage("AiderSettings.xml", roamingType = RoamingType.DISABLED)]
)
class AiderSettings : PersistentStateComponent<AiderSettings.State> {
    private val settingsChangeListeners = mutableListOf<() -> Unit>()

    fun addSettingsChangeListener(listener: () -> Unit) {
        settingsChangeListeners.add(listener)
    }

    fun notifySettingsChanged() {
        settingsChangeListeners.forEach { it() }
    }


    data class State(
        var reasoningEffort: String = AiderDefaults.REASONING_EFFORT,
        var promptAugmentation: Boolean = AiderDefaults.PROMPT_AUGMENTATION,
        var includeCommitMessageBlock: Boolean = AiderDefaults.INCLUDE_COMMIT_MESSAGE_BLOCK,
        var enableDocumentationLookup: Boolean = AiderDefaults.ENABLE_DOCUMENTATION_LOOKUP,
        var useYesFlag: Boolean = AiderDefaults.USE_YES_FLAG,
        var llm: String = AiderDefaults.LLM,
        var additionalArgs: String = AiderDefaults.ADDITIONAL_ARGS,
        var lintCmd: String = AiderDefaults.LINT_CMD,
        var showGitComparisonTool: Boolean = AiderDefaults.SHOW_GIT_COMPARISON_TOOL,
        var activateIdeExecutorAfterWebcrawl: Boolean = AiderDefaults.ACTIVATE_IDE_EXECUTOR_AFTER_WEBCRAWL,
        var webCrawlLlm: String = AiderDefaults.WEB_CRAWL_LLM,
        var deactivateRepoMap: Boolean = AiderDefaults.DEACTIVATE_REPO_MAP,
        var editFormat: String = AiderDefaults.EDIT_FORMAT,
        var verboseCommandLogging: Boolean = AiderDefaults.VERBOSE_COMMAND_LOGGING,
        var useDockerAider: Boolean = AiderDefaults.USE_DOCKER_AIDER,
        var enableMarkdownDialogAutoclose: Boolean = AiderDefaults.ENABLE_MARKDOWN_DIALOG_AUTOCLOSE,
        var markdownDialogAutocloseDelay: Int = AiderDefaults.MARKDOWN_DIALOG_AUTOCLOSE_DELAY_IN_S,
        var mountAiderConfInDocker: Boolean = AiderDefaults.MOUNT_AIDER_CONF_IN_DOCKER,
        var includeChangeContext: Boolean = AiderDefaults.INCLUDE_CHANGE_CONTEXT,
        var autoCommits: AutoCommitSetting = AiderDefaults.AUTO_COMMITS,
        var dirtyCommits: DirtyCommitSetting = AiderDefaults.DIRTY_COMMITS,
        var useSidecarMode: Boolean = AiderDefaults.USE_SIDECAR_MODE,
        var sidecarModeVerbose: Boolean = false, // Enable verbose logging for sidecar
        var alwaysIncludeOpenFiles: Boolean = AiderDefaults.ALWAYS_INCLUDE_OPEN_FILES,
        var alwaysIncludePlanContextFiles: Boolean = AiderDefaults.ALWAYS_INCLUDE_PLAN_CONTEXT_FILES,
        var dockerImage: String = AiderDefaults.DOCKER_IMAGE,
        var aiderExecutablePath: String = AiderDefaults.AIDER_EXECUTABLE_PATH,
        var documentationLlm: String = AiderDefaults.DOCUMENTATION_LLM,
        var enableAutoPlanContinue: Boolean = AiderDefaults.ENABLE_AUTO_PLAN_CONTINUE,
        var enableAutoPlanContinuationInPlanFamily: Boolean = AiderDefaults.ENABLE_AUTO_PLAN_CONTINUATION_IN_FAMILY,
        var optionsPanelCollapsed: Boolean = true,
        var enableLocalModelCostMap: Boolean = false,
        var defaultMode: AiderMode = AiderDefaults.DEFAULT_MODE,
        var pluginBasedEdits: Boolean = AiderDefaults.PLUGIN_BASED_EDITS, // Added for plugin-based edits feature
        var lenientEdits: Boolean = AiderDefaults.LENIENT_EDITS, // Allow processing of multiple edit formats
        var autoCommitAfterEdits: Boolean = AiderDefaults.AUTO_COMMIT_AFTER_EDITS // Auto-commit after plugin-based edits
    )


    var reasoningEffort: String
        get() = myState.reasoningEffort
        set(value) {
            myState.reasoningEffort = value
        }

    var promptAugmentation: Boolean
        get() = myState.promptAugmentation
        set(value) {
            myState.promptAugmentation = value
        }
        
    var includeCommitMessageBlock: Boolean
        get() = myState.includeCommitMessageBlock
        set(value) {
            myState.includeCommitMessageBlock = value
        }

    var documentationLlm: String
        get() = myState.documentationLlm
        set(value) {
            myState.documentationLlm = value
        }


    var enableLocalModelCostMap: Boolean
        get() = myState.enableLocalModelCostMap
        set(value) {
            myState.enableLocalModelCostMap = value
        }

    var defaultMode: AiderMode
        get() = myState.defaultMode
        set(value) {
            myState.defaultMode = value
        }

    var enableAutoPlanContinue: Boolean
        get() = myState.enableAutoPlanContinue
        set(value) {
            myState.enableAutoPlanContinue = value
        }

    var enableAutoPlanContinuationInPlanFamily: Boolean
        get() = myState.enableAutoPlanContinuationInPlanFamily
        set(value) {
            myState.enableAutoPlanContinuationInPlanFamily = value
        }


    private var myState = State()

    var enableDocumentationLookup: Boolean
        get() = myState.enableDocumentationLookup
        set(value) {
            myState.enableDocumentationLookup = value
        }
        
    var enableSubplans: Boolean
        get() = myState.enableSubplans
        set(value) {
            myState.enableSubplans = value
        }

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    var useYesFlag: Boolean
        get() = myState.useYesFlag
        set(value) {
            myState.useYesFlag = value
        }

    var llm: String
        get() = myState.llm
        set(value) {
            myState.llm = value
        }

    var additionalArgs: String
        get() = myState.additionalArgs
        set(value) {
            myState.additionalArgs = value
        }


    var lintCmd: String
        get() = myState.lintCmd
        set(value) {
            myState.lintCmd = value
        }

    var showGitComparisonTool: Boolean
        get() = myState.showGitComparisonTool
        set(value) {
            myState.showGitComparisonTool = value
        }

    var activateIdeExecutorAfterWebcrawl: Boolean
        get() = myState.activateIdeExecutorAfterWebcrawl
        set(value) {
            myState.activateIdeExecutorAfterWebcrawl = value
        }

    var webCrawlLlm: String
        get() = myState.webCrawlLlm
        set(value) {
            myState.webCrawlLlm = value
        }

    var deactivateRepoMap: Boolean
        get() = myState.deactivateRepoMap
        set(value) {
            myState.deactivateRepoMap = value
        }

    var editFormat: String
        get() = myState.editFormat
        set(value) {
            myState.editFormat = value
        }

    var verboseCommandLogging: Boolean
        get() = myState.verboseCommandLogging
        set(value) {
            myState.verboseCommandLogging = value
        }

    var useDockerAider: Boolean
        get() = myState.useDockerAider
        set(value) {
            myState.useDockerAider = value
        }

    var useSidecarMode: Boolean
        get() = myState.useSidecarMode
        set(value) {
            myState.useSidecarMode = value
        }

    var sidecarModeVerbose: Boolean
        get() = myState.sidecarModeVerbose
        set(value) {
            myState.sidecarModeVerbose = value
        }

    var enableMarkdownDialogAutoclose: Boolean
        get() = myState.enableMarkdownDialogAutoclose
        set(value) {
            myState.enableMarkdownDialogAutoclose = value
        }

    var markdownDialogAutocloseDelay: Int
        get() = myState.markdownDialogAutocloseDelay
        set(value) {
            myState.markdownDialogAutocloseDelay = value
        }
    val closeOutputDialogImmediately: Boolean
        get() = markdownDialogAutocloseDelay < 1 && enableMarkdownDialogAutoclose

    var mountAiderConfInDocker: Boolean
        get() = myState.mountAiderConfInDocker
        set(value) {
            myState.mountAiderConfInDocker = value
        }

    var includeChangeContext: Boolean
        get() = myState.includeChangeContext
        set(value) {
            myState.includeChangeContext = value
        }

    var autoCommits: AutoCommitSetting
        get() = myState.autoCommits
        set(value) {
            myState.autoCommits = value
        }

    var dirtyCommits: DirtyCommitSetting
        get() = myState.dirtyCommits
        set(value) {
            myState.dirtyCommits = value
        }

    var alwaysIncludeOpenFiles: Boolean
        get() = myState.alwaysIncludeOpenFiles
        set(value) {
            myState.alwaysIncludeOpenFiles = value
        }

    var alwaysIncludePlanContextFiles: Boolean
        get() = myState.alwaysIncludePlanContextFiles
        set(value) {
            myState.alwaysIncludePlanContextFiles = value
        }

    // Added for plugin-based edits feature
    var pluginBasedEdits: Boolean
        get() = myState.pluginBasedEdits
        set(value) {
            myState.pluginBasedEdits = value
        }

    var lenientEdits: Boolean
        get() = myState.lenientEdits
        set(value) {
            myState.lenientEdits = value
        }
        
    var autoCommitAfterEdits: Boolean
        get() = myState.autoCommitAfterEdits
        set(value) {
            myState.autoCommitAfterEdits = value
        }


    enum class AutoCommitSetting {
        ON, OFF, DEFAULT;

        companion object {
            fun fromIndex(index: Int): AutoCommitSetting = when (index) {
                0 -> DEFAULT
                1 -> ON
                2 -> OFF
                else -> DEFAULT
            }
        }

        fun toIndex(): Int = when (this) {
            DEFAULT -> 0
            ON -> 1
            OFF -> 2
        }
    }

    var dockerImage: String
        get() = myState.dockerImage
        set(value) {
            myState.dockerImage = value
        }


    var aiderExecutablePath: String
        get() = myState.aiderExecutablePath
        set(value) {
            myState.aiderExecutablePath = value
        }

    enum class DirtyCommitSetting {
        ON, OFF, DEFAULT;

        companion object {
            fun fromIndex(index: Int): DirtyCommitSetting = when (index) {
                0 -> DEFAULT
                1 -> ON
                2 -> OFF
                else -> DEFAULT
            }
        }

        fun toIndex(): Int = when (this) {
            DEFAULT -> 0
            ON -> 1
            OFF -> 2
        }
    }

    companion object {
        fun getInstance(): AiderSettings =
            ApplicationManager.getApplication().getService(AiderSettings::class.java)

    }
}
