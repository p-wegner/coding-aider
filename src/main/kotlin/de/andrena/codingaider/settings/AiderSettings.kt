package de.andrena.codingaider.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import de.andrena.codingaider.utils.ApiKeyManager

@Service(Service.Level.PROJECT)
@State(
    name = "de.andrena.codingaider.settings.AiderSettings",
    storages = [Storage("AiderSettings.xml")]
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
        var apiKeys: Map<String, String> = emptyMap()
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
        // Restore API keys from the state
        state.apiKeys.forEach { (keyName, apiKey) ->
            ApiKeyManager.saveApiKey(keyName, apiKey)
        }
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

    var apiKeys: Map<String, String>
        get() = myState.apiKeys
        set(value) {
            myState.apiKeys = value
        }

    companion object {
        fun getInstance(project: Project): AiderSettings =
            project.getService(AiderSettings::class.java)
    }
}
