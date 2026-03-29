package de.andrena.codingaider.executors

import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.strategies.GithubCopilotCliExecutionStrategy
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.settings.AiderSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GithubCopilotCliExecutionStrategyTest {

    private lateinit var strategy: GithubCopilotCliExecutionStrategy
    private lateinit var settings: AiderSettings
    private lateinit var commandData: CommandData

    @BeforeEach
    fun setUp() {
        settings = mock {
            on { ghExecutablePath } doReturn "gh"
            on { copilotProviderBaseUrl } doReturn ""
            on { copilotProviderType } doReturn "openai"
        }
        commandData = CommandData(
            projectPath = "/project",
            files = emptyList(),
            message = "Fix the bug",
            llm = "",
            useYesFlag = true,
            editFormat = "",
            additionalArgs = "",
            lintCmd = "",
            deactivateRepoMap = false,
            sidecarMode = false
        )
        strategy = GithubCopilotCliExecutionStrategy(mock(), settings)
    }

    @Test
    fun `builds basic command with prompt and allow-all-tools`() {
        val command = strategy.buildCommand(commandData)
        assertThat(command).contains("gh", "copilot", "--", "-p", "--allow-all-tools", "--allow-all-paths", "--silent")
        assertThat(command).contains("Fix the bug")
    }

    @Test
    fun `adds project directory via --add-dir`() {
        val command = strategy.buildCommand(commandData)
        assertThat(command).containsSequence("--add-dir", "/project")
    }

    @Test
    fun `adds --model flag when llm is set and no BYOK`() {
        val cmd = strategy.buildCommand(commandData.copy(llm = "gpt-4o"))
        assertThat(cmd).containsSequence("--model", "gpt-4o")
    }

    @Test
    fun `omits --model flag when BYOK base URL is set`() {
        whenever(settings.copilotProviderBaseUrl).thenReturn("http://localhost:11434/v1")
        val cmd = strategy.buildCommand(commandData.copy(llm = "mistral"))
        assertThat(cmd).doesNotContain("--model")
    }

    @Test
    fun `prefixes message with architect for ARCHITECT mode`() {
        val cmd = strategy.buildCommand(commandData.copy(aiderMode = AiderMode.ARCHITECT, message = "Refactor auth"))
        val promptArg = cmd[cmd.indexOf("-p") + 1]
        assertThat(promptArg).contains("/architect Refactor auth")
    }

    @Test
    fun `includes file context in prompt when files are provided`() {
        val cmdData = commandData.copy(
            files = listOf(FileData("/project/src/Main.kt", false))
        )
        val cmd = strategy.buildCommand(cmdData)
        val promptArg = cmd[cmd.indexOf("-p") + 1]
        assertThat(promptArg).contains("Files for context:")
        assertThat(promptArg).contains("/project/src/Main.kt")
    }

    @Test
    fun `replaces real newlines with literal backslash-n in prompt`() {
        val cmdData = commandData.copy(
            message = "Line 1\nLine 2\nLine 3",
            files = listOf(FileData("/project/src/Main.kt", false))
        )
        val cmd = strategy.buildCommand(cmdData)
        val promptArg = cmd[cmd.indexOf("-p") + 1]
        assertThat(promptArg).doesNotContain("\n")
        assertThat(promptArg).contains("\\n")
        assertThat(promptArg).contains("Line 1\\nLine 2\\nLine 3")
    }

    @Test
    @org.junit.jupiter.api.Disabled("Requires IntelliJ application context for ApiKeyManager/PasswordSafe")
    fun `sets BYOK environment variables when provider base URL is configured`() {
        whenever(settings.copilotProviderBaseUrl).thenReturn("https://api.openai.com/v1")
        whenever(settings.copilotProviderType).thenReturn("openai")

        val processBuilder = ProcessBuilder("echo")
        strategy.prepareEnvironment(processBuilder, commandData.copy(llm = "gpt-4"))

        assertThat(processBuilder.environment()).containsEntry("COPILOT_PROVIDER_BASE_URL", "https://api.openai.com/v1")
        assertThat(processBuilder.environment()).containsEntry("COPILOT_PROVIDER_TYPE", "openai")
        assertThat(processBuilder.environment()).containsEntry("COPILOT_MODEL", "gpt-4")
    }

    @Test
    fun `does not set BYOK env vars when provider base URL is empty`() {
        val processBuilder = ProcessBuilder("echo")
        strategy.prepareEnvironment(processBuilder, commandData)
        assertThat(processBuilder.environment()).doesNotContainKey("COPILOT_PROVIDER_BASE_URL")
        assertThat(processBuilder.environment()).doesNotContainKey("COPILOT_MODEL")
    }

    @Test
    fun `uses custom gh executable path from settings`() {
        whenever(settings.ghExecutablePath).thenReturn("/usr/local/bin/gh")
        val cmd = strategy.buildCommand(commandData)
        assertThat(cmd.first()).isEqualTo("/usr/local/bin/gh")
    }

    @Test
    fun `falls back to gh when executable path is blank`() {
        whenever(settings.ghExecutablePath).thenReturn("")
        val cmd = strategy.buildCommand(commandData)
        assertThat(cmd.first()).isEqualTo("gh")
    }
}
