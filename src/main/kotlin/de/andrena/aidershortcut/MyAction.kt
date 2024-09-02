package de.andrena.aidershortcut

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.TerminalExecutionConsole
import com.jediterm.terminal.model.TerminalTextBuffer
import com.jediterm.terminal.model.TerminalLine
import com.jediterm.terminal.model.CharBuffer
import com.jediterm.terminal.TextStyle

class MyAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)

        if (project != null && files != null && files.isNotEmpty()) {
            // Open a dialog with an input field
            val message = Messages.showInputDialog(
                project,
                "Enter your message for aider:",
                "Aider Command",
                Messages.getQuestionIcon()
            )

            if (message != null) {
                // User clicked OK, proceed with the command
                val filePaths = files.joinToString(" ") { it.path }
                val command = "aider -f $filePaths -m \"$message\""

                // Open the Terminal tool window
                val terminalToolWindow = ToolWindowManager.getInstance(project).getToolWindow("Terminal")
                terminalToolWindow?.show {
                    // Get the terminal component
                    val terminalComponent = terminalToolWindow.contentManager.selectedContent?.component
                    if (terminalComponent is TerminalExecutionConsole) {
                        // Insert the command into the terminal
//                        val textBuffer = terminalComponent.terminalTextBuffer
//                        insertCommand(textBuffer, command)
                    } else {
                        // Fallback: show a message with the command if we can't insert it directly
                        Messages.showInfoMessage(project, "Please run this command in the terminal:\n$command", "Aider Command")
                    }
                }
            }
            // If message is null, it means the user cancelled, so we do nothing
        }
    }

    private fun insertCommand(textBuffer: TerminalTextBuffer, command: String) {
        val line = TerminalLine.createEmpty()
        val charBuffer = CharBuffer(command)
        line.writeString(0, charBuffer, TextStyle.EMPTY)
        textBuffer.addLine(line)

        // Add a new line to simulate pressing Enter
        textBuffer.addLine(TerminalLine.createEmpty())
    }

    override fun update(e: AnActionEvent) {
        // Enable the action only if multiple files are selected
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabledAndVisible = project != null && files != null && files.isNotEmpty()
    }
}
