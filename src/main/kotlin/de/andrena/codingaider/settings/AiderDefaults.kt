package de.andrena.codingaider.settings

object AiderDefaults {
    const val SUMMARIZED_OUTPUT = false
    const val USE_SIDECAR_MODE = false
    const val ENABLE_DOCUMENTATION_LOOKUP = false
    const val ALWAYS_INCLUDE_OPEN_FILES = false
    const val ALWAYS_INCLUDE_PLAN_CONTEXT_FILES = true
    const val USE_STRUCTURED_MODE = false
    const val ENABLE_AUTO_PLAN_CONTINUE = true
    const val MARKDOWN_DIALOG_AUTOCLOSE_DELAY_IN_S: Int = 10
    const val USE_YES_FLAG = true
    const val LLM = ""
    const val ADDITIONAL_ARGS = ""
    const val IS_SHELL_MODE = false
    const val LINT_CMD = ""
    const val SHOW_GIT_COMPARISON_TOOL = true
    const val ACTIVATE_IDE_EXECUTOR_AFTER_WEBCRAWL = false
    const val WEB_CRAWL_LLM = "--mini"
    const val DOCUMENTATION_LLM = "Default"
    const val DEACTIVATE_REPO_MAP = false
    const val EDIT_FORMAT = ""
    const val VERBOSE_COMMAND_LOGGING = false
    const val USE_DOCKER_AIDER = false
    const val ENABLE_MARKDOWN_DIALOG_AUTOCLOSE = true
    const val MOUNT_AIDER_CONF_IN_DOCKER = true
    const val INCLUDE_CHANGE_CONTEXT = false
    val AUTO_COMMITS = AiderSettings.AutoCommitSetting.DEFAULT
    val DIRTY_COMMITS = AiderSettings.DirtyCommitSetting.DEFAULT
    const val DOCKER_IMAGE: String = "paulgauthier/aider"
    const val DOCKER_IMAGE_TAG_SUGGESTION: String = "v0.68.0"
    const val AIDER_EXECUTABLE_PATH: String = "aider"
}
