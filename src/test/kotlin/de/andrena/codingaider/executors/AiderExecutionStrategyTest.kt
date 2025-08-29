package de.andrena.codingaider.executors

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.docker.DockerContainerManager
import de.andrena.codingaider.executors.strategies.DockerAiderExecutionStrategy
import de.andrena.codingaider.executors.strategies.NativeAiderExecutionStrategy
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.plans.AiderPlanPromptService
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.settings.AiderDefaults
import de.andrena.codingaider.settings.AiderProjectSettings
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.settings.CustomLlmProviderService
import de.andrena.codingaider.utils.ApiKeyChecker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@Disabled("proper service mocking required")
class AiderExecutionStrategyTest {

    private lateinit var nativeStrategy: NativeAiderExecutionStrategy
    private lateinit var dockerStrategy: DockerAiderExecutionStrategy
    private lateinit var commandData: CommandData
    private lateinit var mockProject: Project
    private lateinit var mockAiderSettings: AiderSettings
    private lateinit var mockDockerManager: DockerContainerManager
    private lateinit var mockApiKeyChecker: ApiKeyChecker
    private lateinit var mockCustomLlmProviderService: CustomLlmProviderService
    private lateinit var structuredModeSystemMessage: String
    private lateinit var multiLineMessage: String

    @BeforeEach
    fun setUp() {
        mockProject = mock()
        mockAiderSettings = mock()
        mockDockerManager = mock()
        mockApiKeyChecker = mock()
        mockCustomLlmProviderService = mock()

        whenever(mockApiKeyChecker.getApiKeysForDocker()).thenReturn(emptyMap())
        whenever(mockProject.getService(AiderProjectSettings::class.java)).thenReturn(mock())
        val aiderSettings = mock<AiderSettings>() {
            on { enableSubplans } doReturn true
        }
        whenever(mockProject.getService(AiderSettings::class.java)).thenReturn(aiderSettings)
        whenever(mockProject.getService(CustomLlmProviderService::class.java)).thenReturn(mockCustomLlmProviderService)
        whenever(mockProject.getService(AiderPlanService::class.java)).thenReturn(AiderPlanService(mockProject))
        whenever(mockProject.getService(AiderPlanPromptService::class.java)).thenReturn(
            AiderPlanPromptService(
                mockProject
            )
        )
        whenever(mockCustomLlmProviderService.getProvider(org.mockito.kotlin.any())).thenReturn(null)

        // Set default behaviors for mocked settings
        whenever(mockAiderSettings.autoCommits).thenReturn(AiderSettings.AutoCommitSetting.DEFAULT)
        whenever(mockAiderSettings.dirtyCommits).thenReturn(AiderSettings.DirtyCommitSetting.DEFAULT)
        whenever(mockAiderSettings.pluginBasedEdits).thenReturn(false)
        whenever(mockAiderSettings.aiderExecutablePath).thenReturn("aider")
        whenever(mockAiderSettings.useSidecarMode).thenReturn(false)
        whenever(mockAiderSettings.mountAiderConfInDocker).thenReturn(true)
        whenever(mockAiderSettings.enableLocalModelCostMap).thenReturn(false)
        whenever(mockAiderSettings.dockerImage).thenReturn(AiderDefaults.DOCKER_IMAGE)

        val resourcesPath = "src/test/resources"
        structuredModeSystemMessage = File("$resourcesPath/structured_mode_system_message.txt").readText().trimIndent()
        multiLineMessage = File("$resourcesPath/multi_line_message.txt").readText().trimIndent()

        dockerStrategy =
            DockerAiderExecutionStrategy(mockProject, mockDockerManager, mockApiKeyChecker, mockAiderSettings)
        nativeStrategy = NativeAiderExecutionStrategy(mockProject, mockApiKeyChecker, mockAiderSettings)
        commandData = CommandData(
            projectPath = "/project",
            files = listOf(FileData("/project/file1.txt", false)),
            message = "Test message",
            llm = "",
            useYesFlag = true,
            editFormat = "diff",
            additionalArgs = "--verbose",
            lintCmd = "lint",
            deactivateRepoMap = true,
            sidecarMode = false
        )
    }




    @Test
    fun `NativeAiderExecutionStrategy builds correct command`() {
        val command = nativeStrategy.buildCommand(commandData)
        assertThat(command).containsExactly(
            "aider",
            "--file",
            "/project/file1.txt",
            "--yes",
            "--edit-format",
            "diff",
            "--no-suggest-shell-commands",
            "--no-pretty",
            "--no-fancy-input",
            "--no-detect-urls",
            "--no-check-update",
            "--verbose",
            "--lint-cmd",
            "lint",
            "--map-tokens",
            "0",
            "-m",
            "Test message"
        )
    }

    @Test
    fun `NativeAiderExecutionStrategy properly uses custom models`() {
        val command = nativeStrategy.buildCommand(commandData.copy(llm = "--4o"))
        assertThat(command).containsExactly(
            "aider",
            "--4o",
            "--file",
            "/project/file1.txt",
            "--yes",
            "--edit-format",
            "diff",
            "--no-suggest-shell-commands",
            "--no-pretty",
            "--no-fancy-input",
            "--no-detect-urls",
            "--no-check-update",
            "--verbose",
            "--lint-cmd",
            "lint",
            "--map-tokens",
            "0",
            "-m",
            "Test message"
        )
    }


    private val DOCKER_IMAGE_WITH_TAG = "paulgauthier/aider:v0.86.1"

    @Test
    fun `DockerAiderExecutionStrategy builds correct command`() {
        whenever(mockDockerManager.getCidFilePath()).thenReturn("/tmp/docker.cid")
        val command = dockerStrategy.buildCommand(commandData.copy(llm = "--4o"))
        val userHome = System.getProperty("user.home")
        assertThat(command).containsExactly(
            "docker", "run", "-i", "--rm",
            "-w", "/app",
            "--cidfile", "/tmp/docker.cid",
            "-v", "/project:/app",
            "-v", "$userHome\\.aider.conf.yml:/app/.aider.conf.yml",
            DOCKER_IMAGE_WITH_TAG,
            "--4o",
            "--file", "/app/file1.txt", "--yes", "--edit-format", "diff",
            "--no-suggest-shell-commands", "--no-pretty",
            "--no-fancy-input", "--no-detect-urls", "--no-check-update", "--verbose", "--lint-cmd", "lint",
            "--map-tokens", "0", "-m", "Test message"
        )
    }

    @Test
    fun `DockerAiderExecutionStrategy builds correct command without llm`() {
        whenever(mockDockerManager.getCidFilePath()).thenReturn("/tmp/docker.cid")
        val command = dockerStrategy.buildCommand(commandData) // commandData already has empty llm
        val userHome = System.getProperty("user.home")
        assertThat(command).containsExactly(
            "docker", "run", "-i", "--rm",
            "-w", "/app",
            "--cidfile", "/tmp/docker.cid",
            "-v", "/project:/app",
            "-v", "$userHome\\.aider.conf.yml:/app/.aider.conf.yml",
            DOCKER_IMAGE_WITH_TAG,
            "--file", "/app/file1.txt", "--yes", "--edit-format", "diff",
            "--no-suggest-shell-commands", "--no-pretty",
            "--no-fancy-input", "--no-detect-urls", "--no-check-update", "--verbose", "--lint-cmd", "lint",
            "--map-tokens", "0", "-m", "Test message"
        )
    }

    @Test
    fun `DockerAiderExecutionStrategy handles files outside project directory`() {
        val outsideFile = FileData("/outside/file2.txt", true)
        commandData = commandData.copy(files = commandData.files + outsideFile)
        whenever(mockDockerManager.getCidFilePath()).thenReturn("/tmp/docker.cid")
        val command = dockerStrategy.buildCommand(commandData)
        assertThat(command).contains("-v", "/outside/file2.txt:/extra/file2.txt")
        assertThat(command).contains("--read", "/extra/file2.txt")
    }

    @ParameterizedTest
    @EnumSource(AiderSettings.AutoCommitSetting::class)
    fun `NativeAiderExecutionStrategy handles auto-commits setting`(autoCommitSetting: AiderSettings.AutoCommitSetting) {
        whenever(mockAiderSettings.autoCommits).thenReturn(autoCommitSetting)
        val command = nativeStrategy.buildCommand(commandData)
        when (autoCommitSetting) {
            AiderSettings.AutoCommitSetting.ON -> assertThat(command).contains("--auto-commits")
            AiderSettings.AutoCommitSetting.OFF -> assertThat(command).contains("--no-auto-commits")
            AiderSettings.AutoCommitSetting.DEFAULT -> assertThat(command).doesNotContain(
                "--auto-commits",
                "--no-auto-commits"
            )
        }
    }

    @ParameterizedTest
    @EnumSource(AiderSettings.DirtyCommitSetting::class)
    fun `NativeAiderExecutionStrategy handles dirty-commits setting`(dirtyCommitSetting: AiderSettings.DirtyCommitSetting) {
        whenever(mockAiderSettings.dirtyCommits).thenReturn(dirtyCommitSetting)
        val command = nativeStrategy.buildCommand(commandData)
        when (dirtyCommitSetting) {
            AiderSettings.DirtyCommitSetting.ON -> assertThat(command).contains("--dirty-commits")
            AiderSettings.DirtyCommitSetting.OFF -> assertThat(command).contains("--no-dirty-commits")
            AiderSettings.DirtyCommitSetting.DEFAULT -> assertThat(command).doesNotContain(
                "--dirty-commits",
                "--no-dirty-commits"
            )
        }
    }

    @Test
    fun `DockerAiderExecutionStrategy handles auto-commits and dirty-commits settings`() {
        whenever(mockAiderSettings.autoCommits).thenReturn(AiderSettings.AutoCommitSetting.ON)
        whenever(mockAiderSettings.dirtyCommits).thenReturn(AiderSettings.DirtyCommitSetting.OFF)
        whenever(mockDockerManager.getCidFilePath()).thenReturn("/tmp/docker.cid")
        val command = dockerStrategy.buildCommand(commandData)
        assertThat(command).contains("--auto-commits")
        assertThat(command).contains("--no-dirty-commits")
    }

    @org.junit.jupiter.api.Nested
    @Disabled("service mocking issues - to be fixed")
    inner class StructuredModeTests {

        @Test
        fun `NativeAiderExecutionStrategy handles structured mode with single-line message`() {
            commandData = commandData.copy(aiderMode = AiderMode.STRUCTURED, message = "Single line message")

            val command = nativeStrategy.buildCommand(commandData)

            assertThat(command).contains("-m")
            assertThat(command.last()).contains("Single line message")
        }

        @Test
        fun `NativeAiderExecutionStrategy handles structured mode with multi-line message`() {
            commandData = commandData.copy(aiderMode = AiderMode.STRUCTURED, message = multiLineMessage)

            val command = nativeStrategy.buildCommand(commandData)

            assertThat(command).contains("-m")
            assertThat(command.last()).isEqualTo("$structuredModeSystemMessage\n<UserPrompt> $multiLineMessage </UserPrompt>")
        }

        @Test
        fun `DockerAiderExecutionStrategy handles structured mode with multi-line message`() {
            commandData = commandData.copy(aiderMode = AiderMode.STRUCTURED, message = multiLineMessage)
            whenever(mockDockerManager.getCidFilePath()).thenReturn("/tmp/docker.cid")
            val command = dockerStrategy.buildCommand(commandData)

            assertThat(command).contains("-m")
            assertThat(command.last()).isEqualTo("$structuredModeSystemMessage\n<UserPrompt> $multiLineMessage </UserPrompt>")
        }

        @Test
        fun `NativeAiderExecutionStrategy handles structured mode with existing plan`() {
            val existingPlanFile = FileData("/project/.coding-aider-plans/existing_plan.md", false)
            commandData = commandData.copy(
                aiderMode = AiderMode.STRUCTURED,
                message = "Continue with the plan",
                files = commandData.files + existingPlanFile
            )

            val command = nativeStrategy.buildCommand(commandData)

            assertThat(command).contains("-m")
            assertThat(command.last()).contains("A plan already exists. Continue implementing the existing plan")
            assertThat(command.last()).contains("Continue with the plan")
        }

        @Test
        fun `DockerAiderExecutionStrategy handles structured mode with existing plan`() {
            val existingPlanFile = FileData("/project/.coding-aider-plans/existing_plan.md", false)
            commandData = commandData.copy(
                aiderMode = AiderMode.STRUCTURED,
                message = "Update the plan",
                files = commandData.files + existingPlanFile
            )
            whenever(mockDockerManager.getCidFilePath()).thenReturn("/tmp/docker.cid")

            val command = dockerStrategy.buildCommand(commandData)

            assertThat(command).contains("-m")
            assertThat(command.last()).contains("A plan already exists. Continue implementing the existing plan")
            assertThat(command.last()).contains("Update the plan")
        }
    }
}
