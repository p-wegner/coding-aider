package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.SidecarAiderExecutionStrategy
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.settings.AiderSettings

@Service(Service.Level.PROJECT)
class SidecarProcessInitializer(private val project: Project) {
    private val logger = Logger.getInstance(SidecarProcessInitializer::class.java)
    private val settings = AiderSettings.getInstance()
    private val processManager = project.service<AiderProcessManager>()

    fun initializeSidecarProcess() {
        // TODO: Add a setting to enable/disable sidecar mode
//        if (!settings.useSidecarMode) {
//            logger.info("Sidecar mode is disabled")
//            return
//        }

        val strategy = SidecarAiderExecutionStrategy(project, settings)
        val command = strategy.buildCommand(createInitializationCommandData())

        val workingDir = project.basePath ?: System.getProperty("user.home")
        val processStarted = processManager.startProcess(command, workingDir)

        if (processStarted) {
            logger.info("Sidecar Aider process initialized successfully")
        } else {
            logger.error("Failed to initialize Sidecar Aider process")
        }
    }

    private fun createInitializationCommandData(): CommandData {
        return CommandData(
            message = "Initialize Aider Sidecar Mode",
            projectPath = project.basePath ?: System.getProperty("user.home"),
            files = emptyList(),
            useYesFlag = false,
            llm = settings.llm,
            additionalArgs = "",
            lintCmd = "",
            aiderMode = AiderMode.NORMAL
        )
    }

    fun shutdownSidecarProcess() {
        processManager.dispose()
    }
}
