package de.andrena.codingaider.settings

import de.andrena.codingaider.inputdialog.AiderMode

object AiderDefaults {
    const val REASONING_EFFORT = "" // Can be: "", "low", "medium", "high"
    const val SUMMARIZED_OUTPUT = false
    const val USE_SIDECAR_MODE = false
    const val ENABLE_DOCUMENTATION_LOOKUP = false
    const val ALWAYS_INCLUDE_OPEN_FILES = false
    const val ALWAYS_INCLUDE_PLAN_CONTEXT_FILES = true
    // TODO: make enum with off, single plan, family
    const val ENABLE_AUTO_PLAN_CONTINUE = true
    const val ENABLE_AUTO_PLAN_CONTINUATION_IN_FAMILY = false
    const val MARKDOWN_DIALOG_AUTOCLOSE_DELAY_IN_S: Int = 10
    const val USE_YES_FLAG = true
    const val LLM = ""
    const val ADDITIONAL_ARGS = ""
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
    val DEFAULT_MODE = AiderMode.NORMAL
    val AUTO_COMMITS = AiderSettings.AutoCommitSetting.DEFAULT
    val DIRTY_COMMITS = AiderSettings.DirtyCommitSetting.DEFAULT
    const val DOCKER_IMAGE: String = "paulgauthier/aider:v0.79.1"
    const val AIDER_EXECUTABLE_PATH: String = "aider"
}
