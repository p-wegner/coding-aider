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
        if (!settings.useSidecarMode) {
            logger.info("Sidecar mode is disabled")
            return
        }

        val strategy = SidecarAiderExecutionStrategy(project, settings)
        val command = strategy.buildCommand(createInitializationCommandData())

        val workingDir = project.basePath ?: System.getProperty("user.home")
        val processStarted = processManager.startProcess(
            command, 
            workingDir, 
            settings.sidecarModeMaxIdleTime, 
            settings.sidecarModeAutoRestart,
            settings.sidecarModeVerbose
        )

        if (processStarted) {
            logger.info("Sidecar Aider process initialized successfully")
            // Schedule periodic health checks if auto-restart is enabled
            if (settings.sidecarModeAutoRestart) {
                scheduleHealthCheck()
            }
        } else {
            logger.error("Failed to initialize Sidecar Aider process")
        }
    }

    private fun scheduleHealthCheck() {
        // TODO: Implement periodic health check for sidecar process
        // This could involve checking process status and restarting if needed
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
