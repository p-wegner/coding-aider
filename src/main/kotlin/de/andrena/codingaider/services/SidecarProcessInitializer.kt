package de.andrena.codingaider.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.CommandOptions
import de.andrena.codingaider.executors.SidecarAiderExecutionStrategy
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.settings.MySettingsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class SidecarProcessInitializer(private val project: Project, private val cs: CoroutineScope) : Disposable {
    private val logger = Logger.getInstance(SidecarProcessInitializer::class.java)
    private val settings = project.service<MySettingsService>().getSettings()
    private val processManager = project.service<AiderProcessManager>()
    private var currentSidecarMode: Boolean? = null

    init {
        // Listen for settings changes
        settings.addSettingsChangeListener {
            handleSettingsChange()
        }
    }

    private fun handleSettingsChange() {
        val newSidecarMode = settings.useSidecarMode

        // Only react if sidecar mode setting has changed
        if (currentSidecarMode != newSidecarMode) {
            if (newSidecarMode) {
                // Sidecar mode enabled, start process
                initializeSidecarProcess()
            } else {
                // Sidecar mode disabled, shutdown process
                shutdownSidecarProcess()
            }
            currentSidecarMode = newSidecarMode
        }
    }

    fun initializeSidecarProcess() {
        if (!settings.useSidecarMode) {
            logger.info("Sidecar mode is disabled")
            return
        }
        if (processManager.isReadyForCommand()) {
            logger.info("Sidecar process is already running")
            return
        }
        logger.info("Starting Sidecar process ")
        cs.launch {
            val strategy = SidecarAiderExecutionStrategy(project, settings)
            val command = strategy.buildCommand(createInitializationCommandData())

            val workingDir = project.basePath ?: System.getProperty("user.home")
            val processStarted = processManager.startProcess(
                command,
                workingDir,
                settings.sidecarModeVerbose,
            )

            if (processStarted) {
                logger.info("Sidecar Aider process initialized successfully")
            } else {
                logger.error("Failed to initialize Sidecar Aider process")
            }

            // Ensure the process is running before returning
            if (!processManager.isReadyForCommand()) {
                throw IllegalStateException("Sidecar Aider process failed to start")
            }
        }
    }

    private fun scheduleHealthCheck() {
        // TODO: Implement periodic health check for sidecar process
        // This could involve checking process status and restarting if needed
    }

    private fun createInitializationCommandData(): CommandData {
        return CommandData(
            message = "",
            projectPath = project.basePath ?: "",
            files = emptyList(),
            useYesFlag = true,
            llm = settings.llm,
            additionalArgs = settings.additionalArgs ?: "",
            lintCmd = settings.lintCmd ?: "",
            aiderMode = AiderMode.NORMAL,
            options = CommandOptions(sidecarMode = true)

        )
    }

    fun shutdownSidecarProcess() {
        processManager.dispose()
        currentSidecarMode = null
    }

    override fun dispose() {
        // Ensure sidecar process is shut down when the project is closed or plugin is unloaded
        shutdownSidecarProcess()
    }
}
