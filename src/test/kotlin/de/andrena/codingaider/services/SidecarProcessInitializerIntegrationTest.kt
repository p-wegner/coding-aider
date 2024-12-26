package de.andrena.codingaider.services

import com.intellij.openapi.project.Project
import de.andrena.codingaider.executors.api.AiderProcessInteractor
import de.andrena.codingaider.executors.api.DefaultAiderProcessInteractor
import de.andrena.codingaider.services.sidecar.AiderProcessManager
import de.andrena.codingaider.services.sidecar.SidecarProcessInitializer
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.settings.MySettingsService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class SidecarProcessInitializerIntegrationTest() : BaseIntegrationTest() {
    @TempDir
    private lateinit var tempDir: java.nio.file.Path
    private lateinit var project: Project
    private lateinit var settings: AiderSettings
    private val settingsService: MySettingsService = mock(MySettingsService::class.java)
    private lateinit var processInteractor: AiderProcessInteractor
    private lateinit var sidecarProcessInitializer: SidecarProcessInitializer
    private lateinit var aiderProcessManager: AiderProcessManager

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeEach
    fun setUp() {
        settings = mock(AiderSettings::class.java)
        project = mock(Project::class.java)
        whenever(settingsService.getSettings()).thenReturn(settings)
        whenever(settings.useSidecarMode).thenReturn(true)
        whenever(settings.sidecarModeVerbose).thenReturn(true)
        whenever(settings.llm).thenReturn("--4o")
        whenever(settings.aiderExecutablePath).thenReturn("aider")
        whenever(settings.autoCommits).thenReturn(AiderSettings.AutoCommitSetting.ON)
        whenever(settings.lintCmd).thenReturn("")
        whenever(settings.additionalArgs).thenReturn("")
        whenever(settings.dirtyCommits).thenReturn(AiderSettings.DirtyCommitSetting.ON)
        whenever(project.getService(MySettingsService::class.java)).thenReturn(settingsService)
        aiderProcessManager = AiderProcessManager()
        whenever(project.getService(AiderProcessManager::class.java)).thenReturn(aiderProcessManager)
        whenever(project.basePath).thenReturn(tempDir.toString())

        processInteractor = DefaultAiderProcessInteractor(project)
        // TODO: would need testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
        //  but depending on coroutines causes problems with the test framework
        // https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#coroutinesLibraries
        // https://youtrack.jetbrains.com/issue/IJPL-164276
//        val testScope = TestScope(UnconfinedTestDispatcher())
//        sidecarProcessInitializer = SidecarProcessInitializer(project, testScope)

    }

    @Test
    @Disabled
    fun testStartAiderWithoutMessageOptionAndSendCommand() = runBlocking {
        sidecarProcessInitializer.initializeSidecarProcess()
        delay(1000) // Give process time to fully initialize

        val response1 = processInteractor.sendCommandSync("What is the meaning of life, the universe, and everything?")
        assertThat(response1).contains("42")

        val response2 = processInteractor.sendCommandSync("What did the fox say?")
        assertThat(response2).contains("Ylvis")

        tearDown()
        Thread.sleep(1000)
    }

    @AfterEach
    fun tearDown() {
        sidecarProcessInitializer.shutdownSidecarProcess()
    }
}
