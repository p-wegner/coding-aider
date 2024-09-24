package de.andrena.codingaider.executors

import com.intellij.openapi.project.Project
import com.jediterm.terminal.ui.JediTermWidget
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider
import de.andrena.codingaider.command.CommandData
import kotlinx.coroutines.runBlocking
import javax.swing.JPanel
import javax.swing.SwingUtilities

class AiderTerminalPanel(
    private val project: Project,
) : JPanel() {
    private val terminal: JediTermWidget
    private val aiderProcessManager: AiderProcessManager = AiderProcessManager(project)

    init {
        val defaultSettingsProvider = DefaultSettingsProvider()
        terminal = JediTermWidget(80, 24, defaultSettingsProvider)
        add(terminal)
        SwingUtilities.invokeLater {
            startAider()
        }
    }

    private fun startAider() {
        runBlocking {
            val commandData = CommandData(project.basePath ?: "", emptyList(), emptyList())
            aiderProcessManager.startAiderProcess(commandData)
        }
        
        terminal.ttyConnector = aiderProcessManager.ttyConnector
        terminal.start()
    }

    fun getAiderProcessManager(): AiderProcessManager = aiderProcessManager
}
