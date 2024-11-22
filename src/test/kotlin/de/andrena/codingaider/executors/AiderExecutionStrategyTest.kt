package de.andrena.codingaider.executors

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.docker.DockerContainerManager
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.plans.AiderPlanPromptService
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.ApiKeyChecker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

class AiderExecutionStrategyTest {

    private lateinit var nativeStrategy: NativeAiderExecutionStrategy
    private lateinit var dockerStrategy: DockerAiderExecutionStrategy
    private lateinit var dockerManager: DockerContainerManager
    private lateinit var project: Project
    private lateinit var apiKeyChecker: ApiKeyChecker
    private lateinit var commandData: CommandData
    private lateinit var aiderSettings: AiderSettings
    private lateinit var aiderPlanService: AiderPlanService
    private lateinit var aiderPlanPromptService: AiderPlanPromptService
    private lateinit var structuredModeSystemMessage: String
    private lateinit var multiLineMessage: String

    @BeforeEach
    fun setup() {
        val resourcesPath = "src/test/resources"
        structuredModeSystemMessage = File("$resourcesPath/structured_mode_system_message.txt").readText().trimIndent()
        multiLineMessage = File("$resourcesPath/multi_line_message.txt").readText().trimIndent()
        apiKeyChecker = mock()
        dockerManager = mock()
        project = mock()
        aiderSettings = AiderSettings()
        aiderPlanService = AiderPlanService(project)
        aiderPlanPromptService = AiderPlanPromptService(project)
        nativeStrategy = NativeAiderExecutionStrategy(project, apiKeyChecker, aiderSettings)
        dockerStrategy = DockerAiderExecutionStrategy(project, dockerManager, apiKeyChecker, aiderSettings)
        commandData = CommandData(
            projectPath = "/project",
            files = listOf(FileData("/project/file1.txt", false)),
            message = "Test message",
            llm = "--4o",
            useYesFlag = true,
            editFormat = "diff",
            additionalArgs = "--verbose",
            lintCmd = "lint",
            deactivateRepoMap = true,
            sidecarMode = false
        )
        whenever(project.getService(AiderPlanService::class.java)).thenReturn(aiderPlanService)
        whenever(project.getService(AiderPlanPromptService::class.java)).thenReturn(aiderPlanPromptService)
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
        assertThat(command.last()).contains("SYSTEM A plan already exists. Continue implementing the existing plan")
        assertThat(command.last()).contains("[STRUCTURED MODE] Continue with the plan")
    }

    @Test
    fun `DockerAiderExecutionStrategy handles structured mode with existing plan`() {
        val existingPlanFile = FileData("/project/.coding-aider-plans/existing_plan.md", false)
        commandData = commandData.copy(
            aiderMode = AiderMode.STRUCTURED,
            message = "Update the plan",
            files = commandData.files + existingPlanFile
        )
        whenever(dockerManager.getCidFilePath()).thenReturn("/tmp/docker.cid")

        val command = dockerStrategy.buildCommand(commandData)

        assertThat(command).contains("-m")
        assertThat(command.last()).contains("SYSTEM A plan already exists. Continue implementing the existing plan")
        assertThat(command.last()).contains("[STRUCTURED MODE] Update the plan")
    }

    @Test
    fun `NativeAiderExecutionStrategy builds correct command`() {
        val command = nativeStrategy.buildCommand(commandData)
        assertThat(command).containsExactly(
            "aider", "--4o", "--file", "/project/file1.txt", "--yes", "--edit-format", "diff",
            "--no-suggest-shell-commands", "--no-pretty", "--no-fancy-input", "--verbose", "--lint-cmd", "lint",
            "--map-tokens", "0", "-m", "Test message"
        )
    }

    @Test
    fun `NativeAiderExecutionStrategy properly uses custom models`() {
        val command = nativeStrategy.buildCommand(commandData.copy(llm = "o1-preview"))
        assertThat(command).containsExactly(
            "aider", "--model", "o1-preview", "--file", "/project/file1.txt", "--yes", "--edit-format", "diff",
            "--no-suggest-shell-commands", "--no-pretty", "--no-fancy-input", "--verbose", "--lint-cmd", "lint",
            "--map-tokens", "0", "-m", "Test message"
        )
    }


    @Test
    fun `DockerAiderExecutionStrategy builds correct command`() {
        whenever(dockerManager.getCidFilePath()).thenReturn("/tmp/docker.cid")
        val command = dockerStrategy.buildCommand(commandData)
        assertThat(command).containsExactly(
            "docker", "run", "-i", "--rm",
            "-w", "/app",
            "--cidfile", "/tmp/docker.cid",
            "-v", "/project:/app",
            "paulgauthier/aider:v0.62.0",
            "--4o", "--file", "/app/file1.txt", "--yes", "--edit-format", "diff",
            "--no-suggest-shell-commands", "--no-pretty",
            "--no-fancy-input", "--verbose", "--lint-cmd", "lint",
            "--map-tokens", "0", "-m", "Test message"
        )
    }

    @Test
    fun `DockerAiderExecutionStrategy handles files outside project directory`() {
        val outsideFile = FileData("/outside/file2.txt", true)
        commandData = commandData.copy(files = commandData.files + outsideFile)
        whenever(dockerManager.getCidFilePath()).thenReturn("/tmp/docker.cid")
        val command = dockerStrategy.buildCommand(commandData)
        assertThat(command).contains("-v", "/outside/file2.txt:/extra/file2.txt")
        assertThat(command).contains("--read", "/extra/file2.txt")
    }

    @ParameterizedTest
    @EnumSource(AiderSettings.AutoCommitSetting::class)
    fun `NativeAiderExecutionStrategy handles auto-commits setting`(autoCommitSetting: AiderSettings.AutoCommitSetting) {
        aiderSettings.autoCommits = autoCommitSetting
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
        aiderSettings.dirtyCommits = dirtyCommitSetting
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
        aiderSettings.autoCommits = AiderSettings.AutoCommitSetting.ON
        aiderSettings.dirtyCommits = AiderSettings.DirtyCommitSetting.OFF
        whenever(dockerManager.getCidFilePath()).thenReturn("/tmp/docker.cid")
        val command = dockerStrategy.buildCommand(commandData)
        assertThat(command).contains("--auto-commits")
        assertThat(command).contains("--no-dirty-commits")
    }

    @Test
    fun `NativeAiderExecutionStrategy handles structured mode with single-line message`() {
        commandData = commandData.copy(aiderMode = AiderMode.STRUCTURED, message = "Single line message")

        val command = nativeStrategy.buildCommand(commandData)

        assertThat(command).contains("-m")
        assertThat(command.last()).contains("[STRUCTURED MODE] Single line message")
    }

    @Test
    fun `NativeAiderExecutionStrategy handles structured mode with multi-line message`() {
        commandData = commandData.copy(aiderMode = AiderMode.STRUCTURED, message = multiLineMessage)

        val command = nativeStrategy.buildCommand(commandData)

        assertThat(command).contains("-m")
        assertThat(command.last()).isEqualTo("$structuredModeSystemMessage\n[STRUCTURED MODE] $multiLineMessage")
    }

    @Test
    fun `DockerAiderExecutionStrategy handles structured mode with multi-line message`() {
        commandData = commandData.copy(aiderMode = AiderMode.STRUCTURED, message = multiLineMessage)
        whenever(dockerManager.getCidFilePath()).thenReturn("/tmp/docker.cid")

        val command = dockerStrategy.buildCommand(commandData)

        assertThat(command).contains("-m")
        assertThat(command.last()).isEqualTo("$structuredModeSystemMessage\n[STRUCTURED MODE] $multiLineMessage")
    }
}
