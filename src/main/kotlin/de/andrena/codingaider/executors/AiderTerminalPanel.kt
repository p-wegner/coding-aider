package de.andrena.codingaider.executors

import com.intellij.openapi.project.Project
import com.jediterm.terminal.ProcessTtyConnector
import com.jediterm.terminal.TtyConnector
import com.jediterm.terminal.ui.JediTermWidget
import com.pty4j.PtyProcessBuilder
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.swing.JPanel
import javax.swing.SwingUtilities

class AiderTerminalPanel(project: Project?) : JPanel() {
    private val terminal: JediTermWidget

    init {
        terminal = JediTermWidget(80, 24)
        add(terminal)

        SwingUtilities.invokeLater {
            try {
                startAider()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @Throws(IOException::class)
    private fun startAider() {
        val envs: MutableMap<String, String> = HashMap(System.getenv())
        envs["TERM"] = "xterm-256color"

        val command = arrayOf("aider")
        val process = PtyProcessBuilder().setCommand(command).setEnvironment(envs).start()

        val connector: TtyConnector = ProcessTtyConnector(process, StandardCharsets.UTF_8)
        terminal.setTtyConnector(connector)
        terminal.start()
    }
}