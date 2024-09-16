package de.andrena.codingaider.executors

import com.intellij.openapi.project.Project

class InteractiveAiderExecutionStrategy(private val project: Project) : AiderExecutionStrategy {
    private var interactiveSession: Process? = null

    override fun execute(command: String, workingDir: String, observer: CommandObserver): Int {
        // TODO: Implement interactive session management
        return 0
    }

    override fun terminate() {
        interactiveSession?.destroy()
        interactiveSession = null
    }
}
