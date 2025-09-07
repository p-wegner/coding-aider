package de.andrena.codingaider.executors

import com.intellij.testFramework.fixtures.BasePlatformTestCase

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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@Disabled
class AiderExecutionStrategyTest : BasePlatformTestCase() {

    private lateinit var nativeStrategy: NativeAiderExecutionStrategy
    private lateinit var dockerStrategy: DockerAiderExecutionStrategy
    private lateinit var commandData: CommandData
    private lateinit var mockAiderSettings: AiderSettings
    private lateinit var mockDockerManager: DockerContainerManager
    private lateinit var mockApiKeyChecker: ApiKeyChecker
    private lateinit var mockCustomLlmProviderService: CustomLlmProviderService
    private lateinit var structuredModeSystemMessage: String
    private lateinit var multiLineMessage: String

    override fun setUp() {
        super.setUp()
        mockDockerManager = mock()
        mockApiKeyChecker = mock()
        mockCustomLlmProviderService = mock()

        mockAiderSettings = mock {
            on { enableSubplans } doReturn true
            on { autoCommits } doReturn AiderSettings.AutoCommitSetting.DEFAULT
            on { dirtyCommits } doReturn AiderSettings.DirtyCommitSetting.DEFAULT
            on { pluginBasedEdits } doReturn false
            on { aiderExecutablePath } doReturn "aider"
            on { useSidecarMode } doReturn false
            on { mountAiderConfInDocker } doReturn true
            on { enableLocalModelCostMap } doReturn false
            on { dockerImage } doReturn AiderDefaults.DOCKER_IMAGE
        }

        whenever(mockApiKeyChecker.getApiKeysForDocker()).thenReturn(emptyMap())
        whenever(mockCustomLlmProviderService.getProvider(any())).thenReturn(null)

        ServiceContainerUtil.replaceService(
            project,
            AiderProjectSettings::class.java,
            mock<AiderProjectSettings>(),
            testRootDisposable
        )
        ServiceContainerUtil.replaceService(project, AiderSettings::class.java, mockAiderSettings, testRootDisposable)
        ServiceContainerUtil.replaceService(
            project,
            CustomLlmProviderService::class.java,
            mockCustomLlmProviderService,
            testRootDisposable
        )
        ServiceContainerUtil.replaceService(
            project,
            AiderPlanService::class.java,
            AiderPlanService(project),
            testRootDisposable
        )
        ServiceContainerUtil.replaceService(
            project,
            AiderPlanPromptService::class.java,
            AiderPlanPromptService(project),
            testRootDisposable
        )


        val resourcesPath = "src/test/resources"
        structuredModeSystemMessage = File("$resourcesPath/structured_mode_system_message.txt").readText().trimIndent()
        multiLineMessage = File("$resourcesPath/multi_line_message.txt").readText().trimIndent()

        dockerStrategy =
            DockerAiderExecutionStrategy(project, mockDockerManager, mockApiKeyChecker, mockAiderSettings)
        nativeStrategy = NativeAiderExecutionStrategy(project, mockApiKeyChecker, mockAiderSettings)
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
    fun testNativeAiderExecutionStrategyBuildsCorrectCommand() {
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
    fun testNativeAiderExecutionStrategyProperlyUsesCustomModels() {
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
    fun testDockerAiderExecutionStrategyBuildsCorrectCommand() {
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
            "--map-tokens",
            "0",
            "-m",
            "Test message"
        )
    }

    @Test
    fun testDockerAiderExecutionStrategyBuildsCorrectCommandWithoutLlm() {
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
            "--map-tokens",
            "0",
            "-m",
            "Test message"
        )
    }

    @Test
    fun `test docker aider execution strategy handles files outside project directory`() {
        val outsideFile = FileData("/outside/file2.txt", true)
        commandData = commandData.copy(files = commandData.files + outsideFile)
        whenever(mockDockerManager.getCidFilePath()).thenReturn("/tmp/docker.cid")
        val command = dockerStrategy.buildCommand(commandData)
        assertThat(command).contains("-v", "/outside/file2.txt:/extra/file2.txt")
        assertThat(command).contains("--read", "/extra/file2.txt")
    }

    @Test
    fun `test native aider execution strategy handles auto commits setting on`() {
        whenever(mockAiderSettings.autoCommits).thenReturn(AiderSettings.AutoCommitSetting.ON)
        val command = nativeStrategy.buildCommand(commandData)
        assertThat(command).contains("--auto-commits")
    }

    @Test
    fun testNativeAiderExecutionStrategyHandlesAutoCommitsSettingOFF() {
        whenever(mockAiderSettings.autoCommits).thenReturn(AiderSettings.AutoCommitSetting.OFF)
        val command = nativeStrategy.buildCommand(commandData)
        assertThat(command).contains("--no-auto-commits")
    }

    @Test
    fun testNativeAiderExecutionStrategyHandlesAutoCommitsSettingDEFAULT() {
        whenever(mockAiderSettings.autoCommits).thenReturn(AiderSettings.AutoCommitSetting.DEFAULT)
        val command = nativeStrategy.buildCommand(commandData)
        assertThat(command).doesNotContain("--auto-commits", "--no-auto-commits")
    }

    @Test
    fun testNativeAiderExecutionStrategyHandlesDirtyCommitsSettingON() {
        whenever(mockAiderSettings.dirtyCommits).thenReturn(AiderSettings.DirtyCommitSetting.ON)
        val command = nativeStrategy.buildCommand(commandData)
        assertThat(command).contains("--dirty-commits")
    }

    @Test
    fun testNativeAiderExecutionStrategyHandlesDirtyCommitsSettingOFF() {
        whenever(mockAiderSettings.dirtyCommits).thenReturn(AiderSettings.DirtyCommitSetting.OFF)
        val command = nativeStrategy.buildCommand(commandData)
        assertThat(command).contains("--no-dirty-commits")
    }

    @Test
    fun testNativeAiderExecutionStrategyHandlesDirtyCommitsSettingDEFAULT() {
        whenever(mockAiderSettings.dirtyCommits).thenReturn(AiderSettings.DirtyCommitSetting.DEFAULT)
        val command = nativeStrategy.buildCommand(commandData)
        assertThat(command).doesNotContain("--dirty-commits", "--no-dirty-commits")
    }

    @Test
    fun testDockerAiderExecutionStrategyHandlesAutoCommitsAndDirtyCommitsSettings() {
        whenever(mockAiderSettings.autoCommits).thenReturn(AiderSettings.AutoCommitSetting.ON)
        whenever(mockAiderSettings.dirtyCommits).thenReturn(AiderSettings.DirtyCommitSetting.OFF)
        whenever(mockDockerManager.getCidFilePath()).thenReturn("/tmp/docker.cid")
        val command = dockerStrategy.buildCommand(commandData)
        assertThat(command).contains("--auto-commits")
        assertThat(command).contains("--no-dirty-commits")
    }

    @Test
    fun testNativeAiderExecutionStrategyHandlesStructuredModeWithSingleLineMessage() {
        commandData = commandData.copy(aiderMode = AiderMode.STRUCTURED, message = "Single line message")

        val command = nativeStrategy.buildCommand(commandData)

        assertThat(command).contains("-m")
        assertThat(command.last()).contains("Single line message")
    }

    @Test
    fun testNativeAiderExecutionStrategyHandlesStructuredModeWithMultiLineMessage() {
        commandData = commandData.copy(aiderMode = AiderMode.STRUCTURED, message = multiLineMessage)

        val command = nativeStrategy.buildCommand(commandData)

        assertThat(command).contains("-m")
        assertThat(command.last()).isEqualTo("$structuredModeSystemMessage\n<UserPrompt> $multiLineMessage </UserPrompt>")
    }

    @Test
    fun testDockerAiderExecutionStrategyHandlesStructuredModeWithMultiLineMessage() {
        commandData = commandData.copy(aiderMode = AiderMode.STRUCTURED, message = multiLineMessage)
        whenever(mockDockerManager.getCidFilePath()).thenReturn("/tmp/docker.cid")
        val command = dockerStrategy.buildCommand(commandData)

        assertThat(command).contains("-m")
        assertThat(command.last()).isEqualTo("$structuredModeSystemMessage\n<UserPrompt> $multiLineMessage </UserPrompt>")
    }

    @Test
    fun testNativeAiderExecutionStrategyHandlesStructuredModeWithExistingPlan() {
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
    fun testDockerAiderExecutionStrategyHandlesStructuredModeWithExistingPlan() {
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
