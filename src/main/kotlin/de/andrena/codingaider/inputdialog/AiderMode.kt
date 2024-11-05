package de.andrena.codingaider.inputdialog

import com.intellij.icons.AllIcons
import javax.swing.Icon

const val structuredModeTooltip =
    "<html>Enable structured mode for organized feature development:<br>" + "1. Describe a feature to generate a plan and checklist<br>" + "2. Plans are stored in .coding-aider-plans directory<br>" + "3. Aider updates plans based on progress and new requirements<br>" + "4. Implements plan step-by-step when in context<br>" + "5. Message can be left empty to continue with an existing plan<br>" + "Use for better tracking and systematic development</html>"

private const val architectureModeTooltip =
    "<html>Enable architecture mode for cases where single shot prompting is not sufficient:<br>" + "The mode uses two requests to mimic a lightweight chain of thought and a more structured approach to problem solving.<br>" + "The first request will give the model more freedom to think and generate a plan.<br>" + "The second request will be used to convert the resulting solution into a code implementation.<br>"

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