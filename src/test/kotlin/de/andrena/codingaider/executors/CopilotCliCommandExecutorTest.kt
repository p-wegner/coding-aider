package de.andrena.codingaider.executors

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.strategies.GithubCopilotCliExecutionStrategy
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.ApiKeyChecker
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/**
 * Integration tests for CommandExecutor behaviour specific to the GitHub Copilot CLI strategy.
 * These tests require the IntelliJ test framework and are disabled by default.
 */
@Disabled
class CopilotCliCommandExecutorTest : BasePlatformTestCase() {

    private lateinit var mockSettings: AiderSettings
    private lateinit var mockApiKeyChecker: ApiKeyChecker

    override fun setUp() {
        super.setUp()
        mockApiKeyChecker = mock()
        mockSettings = mock {
            on { useCopilotCli } doReturn true
            on { ghExecutablePath } doReturn "gh"
            on { copilotProviderBaseUrl } doReturn ""
            on { useSidecarMode } doReturn false
        }
        ServiceContainerUtil.replaceService(project, AiderSettings::class.java, mockSettings, testRootDisposable)
    }

    @Test
    fun `executeCommand throws when sidecarMode is true and useCopilotCli is true`() {
        val commandData = CommandData(
            projectPath = project.basePath ?: "/tmp",
            files = emptyList(),
            message = "test",
            llm = "",
            useYesFlag = false,
            editFormat = "",
            additionalArgs = "",
            lintCmd = "",
            deactivateRepoMap = false,
            sidecarMode = true
        )

        val executor = CommandExecutor(commandData, project, mockApiKeyChecker)

        assertThatThrownBy { executor.executeCommand() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Sidecar mode is not supported with the GitHub Copilot CLI strategy")
    }

    @Test
    fun `GithubCopilotCliExecutionStrategy is selected when useCopilotCli is true`() {
        val strategySettings = mock<AiderSettings> {
            on { ghExecutablePath } doReturn "gh"
            on { copilotProviderBaseUrl } doReturn ""
            on { useCopilotCli } doReturn true
        }
        val strategy = GithubCopilotCliExecutionStrategy(project, strategySettings)

        val commandData = CommandData(
            projectPath = "/project",
            files = emptyList(),
            message = "Hello",
            llm = "",
            useYesFlag = false,
            editFormat = "",
            additionalArgs = "",
            lintCmd = "",
            deactivateRepoMap = false,
            sidecarMode = false
        )

        val command = strategy.buildCommand(commandData)
        assertThat(command).contains("gh", "copilot", "--", "--allow-all-tools")
    }
}
