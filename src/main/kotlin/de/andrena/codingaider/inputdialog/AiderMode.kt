package de.andrena.codingaider.inputdialog

import com.intellij.icons.AllIcons
import javax.swing.Icon

private val structuredModeTooltip = """
    <html>Enable structured mode for organized feature development:<br>
    1. Describe a feature to generate a plan and checklist<br>
    2. Plans are stored in .coding-aider-plans directory<br>
    3. (Optional) Review and update the plan and checklist manually<br>
    4. Aider updates the checklist automatically and will usually continue with the next unchecked steps<br>
    5. Implements plan step-by-step when in context<br>
    6. Message can be left empty to continue with an existing plan<br>
    Use for better tracking and systematic development</html>
""".trimIndent()

private val architectureModeTooltip = """
    <html>Enable architecture mode for cases where single shot prompting is not sufficient.<br>
    The mode uses two requests to mimic a lightweight chain of thought and a more structured approach to problem solving.<br>
    The first request will give the model more freedom to think and generate a plan.<br>
    The second request will be used to convert the resulting solution into a code implementation.<br>
""".trimIndent()

enum class AiderMode(
    val displayName: String,
    val tooltip: String,
    val icon: Icon,

    ) {
    NORMAL("Normal", "Standard AI code assistance", AllIcons.Actions.Edit),
    ARCHITECT(
        "Architect",
        architectureModeTooltip,
        AllIcons.Actions.Search
    ),
    STRUCTURED("Structured", structuredModeTooltip, AllIcons.Actions.ListFiles),
    SHELL(
        "Shell",
        "Execute shell commands",
        AllIcons.Debugger.Console
    ),
}
