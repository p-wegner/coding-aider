package de.andrena.codingaider.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*

@Service(Service.Level.APP)
@State(
    name = "de.andrena.codingaider.settings.AiderSettings",
    storages = [Storage("AiderSettings.xml", roamingType = RoamingType.DISABLED)]
)
class AiderSettings : PersistentStateComponent<AiderSettings.State> {
    data class State(
        var useYesFlag: Boolean = AiderDefaults.USE_YES_FLAG,
        var llm: String = AiderDefaults.LLM,
        var additionalArgs: String = AiderDefaults.ADDITIONAL_ARGS,
        var isShellMode: Boolean = AiderDefaults.IS_SHELL_MODE,
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
        var useStructuredMode: Boolean = AiderDefaults.USE_STRUCTURED_MODE,
        var alwaysIncludeOpenFiles: Boolean = AiderDefaults.ALWAYS_INCLUDE_OPEN_FILES,
        var dockerImageTag: String = AiderDefaults.DOCKER_IMAGE_TAG_SUGGESTION,
    )

    private var myState = State()

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

    var isShellMode: Boolean
        get() = myState.isShellMode
        set(value) {
            myState.isShellMode = value
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

    var useStructuredMode: Boolean
        get() = myState.useStructuredMode
        set(value) {
            myState.useStructuredMode = value
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

    var dockerImageTag: String
        get() = myState.dockerImageTag
        set(value) {
            myState.dockerImageTag = value
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
