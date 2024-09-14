package de.andrena.codingaider.executors

import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.docker.DockerContainerManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AiderExecutionStrategyTest {

    private lateinit var nativeStrategy: NativeAiderExecutionStrategy
    private lateinit var dockerStrategy: DockerAiderExecutionStrategy
    private lateinit var dockerManager: DockerContainerManager
    private lateinit var commandData: CommandData

    @BeforeEach
    fun setup() {
        nativeStrategy = NativeAiderExecutionStrategy()
        dockerManager = mock()
        dockerStrategy = DockerAiderExecutionStrategy(dockerManager)
        commandData = CommandData(
            projectPath = "/project",
            files = listOf(FileData("/project/file1.txt", false)),
            message = "Test message",
            llm = "gpt-4",
            useYesFlag = true,
            editFormat = "diff",
            isShellMode = false,
            additionalArgs = "--verbose",
            lintCmd = "lint",
            deactivateRepoMap = true
        )
    }

    @Test
    fun `NativeAiderExecutionStrategy builds correct command`() {
        val command = nativeStrategy.buildCommand(commandData)
        assertThat(command).containsExactly(
            "aider", "gpt-4", "--file", "/project/file1.txt", "--yes", "--edit-format", "diff",
            "--no-suggest-shell-commands", "--no-pretty", "--verbose", "--lint-cmd", "lint",
            "--map-tokens", "0", "-m", "Test message"
        )
    }

    @Test
    fun `DockerAiderExecutionStrategy builds correct command`() {
        whenever(dockerManager.getCidFilePath()).thenReturn("/tmp/docker.cid")
        val command = dockerStrategy.buildCommand(commandData)
        assertThat(command).containsExactly(
            "docker", "run", "-i", "--rm",
            "-v", "/project:/app",
            "-w", "/app",
            "--cidfile", "/tmp/docker.cid",
            "ghcr.io/paul-gauthier/aider:latest",
            "gpt-4", "--file", "/app/file1.txt", "--yes", "--edit-format", "diff",
            "--no-suggest-shell-commands", "--no-pretty", "--verbose", "--lint-cmd", "lint",
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
}
