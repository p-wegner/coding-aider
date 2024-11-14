package de.andrena.codingaider.services

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.CommandOptions
import de.andrena.codingaider.executors.api.AiderProcessInteractor
import de.andrena.codingaider.executors.api.DefaultAiderProcessInteractor
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.settings.AiderSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever

class SidecarProcessInitializerIntegrationTest(@TempDir val tempDir: java.nio.file.Path) : BaseIntegrationTest() {

    private lateinit var project: Project
    private lateinit var settings: AiderSettings
    private val settingsService: MySettingsService = mock(MySettingsService::class.java)
    private lateinit var processInteractor: AiderProcessInteractor
    private lateinit var sidecarProcessInitializer: SidecarProcessInitializer
    private lateinit var aiderProcessManager: AiderProcessManager

    @BeforeEach
    fun setUp() {
        settings = mock(AiderSettings::class.java)
        project = mock(Project::class.java)
        whenever(settingsService.getSettings()).thenReturn(settings)
        whenever(settings.useSidecarMode).thenReturn(true)
        whenever(settings.sidecarModeMaxIdleTime).thenReturn(60)
        whenever(settings.sidecarModeAutoRestart).thenReturn(true)
        whenever(settings.sidecarModeVerbose).thenReturn(true)
        whenever(settings.llm).thenReturn("--4o")
        whenever(settings.aiderExecutablePath).thenReturn("aider")
        whenever(settings.autoCommits).thenReturn(AiderSettings.AutoCommitSetting.ON)
        whenever(settings.dirtyCommits).thenReturn(AiderSettings.DirtyCommitSetting.ON)
        whenever(project.getService(MySettingsService::class.java)).thenReturn(settingsService)
        aiderProcessManager = AiderProcessManager(project)
        whenever(project.getService(AiderProcessManager::class.java)).thenReturn(aiderProcessManager)
        whenever(project.basePath).thenReturn(tempDir.toString())

        processInteractor = DefaultAiderProcessInteractor(project)
        sidecarProcessInitializer = SidecarProcessInitializer(project)

    }

    @Test
    fun testStartAiderWithoutMessageOptionAndSendCommand() {
        // Arrange
        val commandData = CommandData(
            message = "",
            useYesFlag = true,
            llm = "gpt-4o",
            additionalArgs = "",
            files = emptyList(),
            lintCmd = "",
            deactivateRepoMap = false,
            editFormat = "",
            projectPath = project.basePath ?: "",
            options = CommandOptions.DEFAULT,
            aiderMode = AiderMode.NORMAL
        )

        // Act
        sidecarProcessInitializer.initializeSidecarProcess()
        val response = processInteractor.sendCommand("echo 'Hello, Aider!'")

        // Assert
        assertThat(response).contains("Hello, Aider!")
    }
}
