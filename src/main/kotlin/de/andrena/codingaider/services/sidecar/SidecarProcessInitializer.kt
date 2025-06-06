package de.andrena.codingaider.services.sidecar

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.strategies.SidecarAiderExecutionStrategy
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.settings.MySettingsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

@Service(Service.Level.PROJECT)
class SidecarProcessInitializer(private val project: Project, private val cs: CoroutineScope) : Disposable {
    private val logger = Logger.getInstance(SidecarProcessInitializer::class.java)
    private val settings = project.service<MySettingsService>().getSettings()
    private val processManager = project.service<AiderProcessManager>()
    private var currentSidecarMode: Boolean? = null

    init {
        settings.addSettingsChangeListener { handleSettingsChange() }
    }

    private fun handleSettingsChange() {
        val newSidecarMode = settings.useSidecarMode

        if (currentSidecarMode != newSidecarMode) {
            if (newSidecarMode) {
                initializeSidecarProcess()
            } else {
                shutdownSidecarProcess()
            }
            currentSidecarMode = newSidecarMode
        }
    }

    fun initializeSidecarProcess(planId: String? = null) {
        if (!settings.useSidecarMode) {
            logger.info("Sidecar mode is disabled")
            return
        }
        if (processManager.isReadyForCommand(planId)) {
            logger.info("Sidecar process is already running")
            return
        }
        logger.info("Starting Sidecar process ")
        val strategy = SidecarAiderExecutionStrategy(project, settings)

        val command = strategy.buildCommand(createInitializationCommandData(planId))

        val workingDir = project.basePath ?: System.getProperty("user.home")
        // TODO: set env as needed
        processManager.startProcess(
            command,
            workingDir,
            settings.sidecarModeVerbose,
            planId
        ).block()
    }

    private fun createInitializationCommandData(planId: String?): CommandData {
        if (planId == null) {
            return CommandData(
                message = "",
                projectPath = project.basePath ?: "",
                files = emptyList(),
                useYesFlag = true,
                llm = settings.llm,
                additionalArgs = settings.additionalArgs ?: "",
                lintCmd = settings.lintCmd ?: "",
                aiderMode = AiderMode.NORMAL,
                sidecarMode = settings.useSidecarMode

            )
        }
        val aiderPlan = project.service<AiderPlanService>().loadPlanFromFile(File(planId))
            ?: throw IllegalStateException("Plan not found")
        val files = aiderPlan.allFiles.filter { fileData ->
            val file = File(fileData.filePath)
            file.exists()
        }
        return CommandData(
            message = "",
            projectPath = project.basePath ?: "",
            files = files,
            useYesFlag = true,
            llm = settings.llm,
            additionalArgs = settings.additionalArgs ?: "",
            lintCmd = settings.lintCmd ?: "",
            aiderMode = AiderMode.STRUCTURED,
            sidecarMode = settings.useSidecarMode,
            planId = planId
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
