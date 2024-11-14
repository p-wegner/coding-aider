package de.andrena.codingaider.services

import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.api.AiderProcessInteractor
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.settings.AiderSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class SidecarProcessInitializerIntegrationTest : BasePlatformTestCase() {

    private lateinit var project: Project
    private lateinit var settings: AiderSettings
    private lateinit var processInteractor: AiderProcessInteractor
    private lateinit var sidecarProcessInitializer: SidecarProcessInitializer

    @BeforeEach
    fun setUp() {
        project = Mockito.mock(Project::class.java)
        settings = Mockito.mock(AiderSettings::class.java)
        processInteractor = Mockito.mock(AiderProcessInteractor::class.java)
        sidecarProcessInitializer = SidecarProcessInitializer(project)

        `when`(settings.useSidecarMode).thenReturn(true)
        `when`(settings.sidecarModeMaxIdleTime).thenReturn(60)
        `when`(settings.sidecarModeAutoRestart).thenReturn(true)
        `when`(settings.sidecarModeVerbose).thenReturn(true)
    }

    @Test
    fun testStartAiderWithoutMessageOptionAndSendCommand() {
        // Arrange
        val commandData = CommandData(
            message = "",
            useYesFlag = true,
            llm = "gpt-4",
            additionalArgs = "",
            files = emptyList(),
            lintCmd = "",
            deactivateRepoMap = false,
            editFormat = "",
            projectPath = project.basePath ?: System.getProperty("user.home"),
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
