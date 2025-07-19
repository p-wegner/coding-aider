package de.andrena.codingaider.settings

import de.andrena.codingaider.inputdialog.AiderMode

object AiderDefaults {
    const val REASONING_EFFORT = "" // Can be: "", "low", "medium", "high"
    const val PROMPT_AUGMENTATION = false
    const val INCLUDE_COMMIT_MESSAGE_BLOCK = false
    const val USE_SIDECAR_MODE = false
    const val ENABLE_DOCUMENTATION_LOOKUP = false
    const val ALWAYS_INCLUDE_OPEN_FILES = false
    const val ALWAYS_INCLUDE_PLAN_CONTEXT_FILES = true
    const val ENABLE_SUBPLANS = true

    // TODO:  Format instruction for plugin-based edits
    const val PLUGIN_BASED_EDITS_INSTRUCTION = """
When making code changes, please format them as SEARCH/REPLACE blocks using this format:

filepath
```language
<<<<<<< SEARCH
code to search for
=======
code to replace with
>>>>>>> REPLACE
```

For each change:
1. Start with the relative file path on its own line
2. Use triple backticks with the language name
3. Include <<<<<<< SEARCH followed by the exact code to find
4. Use ======= as a separator
5. Include the new code after the separator
6. End with >>>>>>> REPLACE
7. Close with triple backticks

To create a new file, use an empty SEARCH section.
Make your changes precise and minimal."""
    // TODO: make enum with off, single plan, family
    const val ENABLE_AUTO_PLAN_CONTINUE = true
    const val ENABLE_AUTO_PLAN_CONTINUATION_IN_FAMILY = false
    const val USE_YES_FLAG = true
    const val LLM = ""
    const val ADDITIONAL_ARGS = ""
    const val LINT_CMD = ""
    const val SHOW_GIT_COMPARISON_TOOL = true
    const val ACTIVATE_IDE_EXECUTOR_AFTER_WEBCRAWL = false
    const val WEB_CRAWL_LLM = "--mini"
    const val DOCUMENTATION_LLM = "Default"
    const val PLAN_REFINEMENT_LLM = ""
    const val DEACTIVATE_REPO_MAP = false
    const val EDIT_FORMAT = ""
    const val VERBOSE_COMMAND_LOGGING = false
    const val USE_DOCKER_AIDER = false
    const val MOUNT_AIDER_CONF_IN_DOCKER = true
    const val INCLUDE_CHANGE_CONTEXT = false
    val DEFAULT_MODE = AiderMode.NORMAL
    val AUTO_COMMITS = AiderSettings.AutoCommitSetting.DEFAULT
    val DIRTY_COMMITS = AiderSettings.DirtyCommitSetting.DEFAULT
    const val DOCKER_IMAGE: String = "paulgauthier/aider:v0.84.0"
    const val AIDER_EXECUTABLE_PATH: String = "aider"
    const val PLUGIN_BASED_EDITS = false // Added for plugin-based edits feature
    const val LENIENT_EDITS = false // Allow processing of multiple edit formats in a single response
    const val AUTO_COMMIT_AFTER_EDITS = false // Auto-commit after plugin-based edits
    const val SHOW_WORKING_DIRECTORY_PANEL = true // Show working directory panel in tool window
    const val SHOW_DEV_TOOLS = false // Show DevTools button in markdown viewer
    
    // MCP Server settings
    const val ENABLE_MCP_SERVER = true // Enable MCP server for persistent files
    const val MCP_SERVER_PORT = 8080 // Default port for MCP server
    const val MCP_SERVER_AUTO_START = true // Auto-start MCP server with plugin
}
