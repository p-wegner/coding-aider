package de.andrena.aidershortcut

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

class ShellExecutor(private val project: Project, private val commandData: CommandData) {
    fun execute() {
        val terminalView = TerminalToolWindowManager.getInstance(project)
        val terminalSession = terminalView.createLocalShellWidget(project.basePath, "Aider", true)

        val command = buildAiderCommand(commandData, true)
        terminalSession.executeCommand(command)
    }

    private fun buildAiderCommand(commandData: CommandData, isShellMode: Boolean): String {
        return StringBuilder("aider ${commandData.selectedCommand}").apply {
            if (commandData.filePaths.isNotBlank()) append(" --file ${commandData.filePaths}")
            if (commandData.useYesFlag) append(" --yes")
            if (!isShellMode) {
                append(" -m \"${commandData.message}\"")
                append(" --no-suggest-shell-commands")
            }
            if (commandData.readOnlyFiles.isNotEmpty()) append(" --read ${commandData.readOnlyFiles.joinToString(" ")}")
            if (commandData.additionalArgs.isNotEmpty()) append(" ${commandData.additionalArgs}")
        }.toString()
    }
}
package de.andrena.aidershortcut

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.*

class ReadOnlyFilesView(private val files: List<String>, private val persistentFiles: List<String>) : JPanel() {
    private val leftList = JList<String>(files.toTypedArray())
    private val rightList = JList<String>(persistentFiles.toTypedArray())
    private val leftModel = DefaultListModel<String>()
    private val rightModel = DefaultListModel<String>()

    init {
        layout = BorderLayout()
        
        leftList.model = leftModel
        rightList.model = rightModel
        
        files.forEach { leftModel.addElement(it) }
        persistentFiles.forEach { rightModel.addElement(it) }

        val leftPanel = JPanel(BorderLayout())
        leftPanel.add(JLabel("Available Files:"), BorderLayout.NORTH)
        leftPanel.add(JScrollPane(leftList), BorderLayout.CENTER)

        val rightPanel = JPanel(BorderLayout())
        rightPanel.add(JLabel("Persistent Files:"), BorderLayout.NORTH)
        rightPanel.add(JScrollPane(rightList), BorderLayout.CENTER)

        val buttonPanel = JPanel(GridLayout(2, 1))
        val addButton = JButton(">>")
        val removeButton = JButton("<<")

        addButton.addActionListener {
            leftList.selectedValuesList.forEach {
                leftModel.removeElement(it)
                rightModel.addElement(it)
            }
        }

        removeButton.addActionListener {
            rightList.selectedValuesList.forEach {
                rightModel.removeElement(it)
                leftModel.addElement(it)
            }
        }

        buttonPanel.add(addButton)
        buttonPanel.add(removeButton)

        add(leftPanel, BorderLayout.WEST)
        add(buttonPanel, BorderLayout.CENTER)
        add(rightPanel, BorderLayout.EAST)

        preferredSize = Dimension(600, 300)
    }

    fun getPersistentFiles(): List<String> = rightModel.elements().toList()
}
