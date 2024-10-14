package de.andrena.codingaider.executors

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.docker.DockerContainerManager
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.ApiKeyChecker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import de.andrena.codingaider.services.AiderPlanService

class AiderExecutionStrategyTest {

    private lateinit var nativeStrategy: NativeAiderExecutionStrategy
    private lateinit var dockerStrategy: DockerAiderExecutionStrategy
    private lateinit var dockerManager: DockerContainerManager
    private lateinit var project: Project
    private lateinit var apiKeyChecker: ApiKeyChecker
    private lateinit var commandData: CommandData
    private lateinit var aiderSettings: AiderSettings
    private lateinit var aiderPlanService: AiderPlanService

    @BeforeEach
    fun setup() {
        apiKeyChecker = mock()
        dockerManager = mock()
        project = mock()
        aiderSettings = AiderSettings()
        aiderPlanService = AiderPlanService(project)
        nativeStrategy = NativeAiderExecutionStrategy(project, apiKeyChecker, aiderSettings)
        dockerStrategy = DockerAiderExecutionStrategy(project, dockerManager, apiKeyChecker, aiderSettings)
        commandData = CommandData(
            projectPath = "/project",
            files = listOf(FileData("/project/file1.txt", false)),
            message = "Test message",
            llm = "--4o",
            useYesFlag = true,
            editFormat = "diff",
            isShellMode = false,
            additionalArgs = "--verbose",
            lintCmd = "lint",
            deactivateRepoMap = true,
            structuredMode = false
        )
        whenever(project.getService(AiderPlanService::class.java)).thenReturn(aiderPlanService)
    }

    @Test
    fun `NativeAiderExecutionStrategy builds correct command`() {
        val command = nativeStrategy.buildCommand(commandData)
        assertThat(command).containsExactly(
            "aider", "--4o", "--file", "/project/file1.txt", "--yes", "--edit-format", "diff",
            "--no-suggest-shell-commands", "--no-pretty", "--verbose", "--lint-cmd", "lint",
            "--map-tokens", "0", "-m", "Test message"
        )
    }

    @Test
    fun `NativeAiderExecutionStrategy properly uses custom models`() {
        val command = nativeStrategy.buildCommand(commandData.copy(llm = "o1-preview"))
        assertThat(command).containsExactly(
            "aider", "--model", "o1-preview", "--file", "/project/file1.txt", "--yes", "--edit-format", "diff",
            "--no-suggest-shell-commands", "--no-pretty", "--verbose", "--lint-cmd", "lint",
            "--map-tokens", "0", "-m", "Test message"
        )
    }


    @Test
    fun `DockerAiderExecutionStrategy builds correct command`() {
        whenever(dockerManager.getCidFilePath()).thenReturn("/tmp/docker.cid")
        val command = dockerStrategy.buildCommand(commandData)
        assertThat(command).containsExactly(
            "docker", "run", "-i", "--rm", "-t",
            "-v", "/project:/app",
            "-w", "/app",
            "--cidfile", "/tmp/docker.cid",
            "paulgauthier/aider:v0.57.1",
            "--4o", "--file", "/app/file1.txt", "--yes", "--edit-format", "diff",
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
        commandData = commandData.copy(structuredMode = true, message = "Single line message")
        
        val command = nativeStrategy.buildCommand(commandData)
        
        assertThat(command).contains("-m")
        assertThat(command.last()).contains("[STRUCTURED MODE] Single line message")
    }

    @Test
    fun `NativeAiderExecutionStrategy handles structured mode with multi-line message`() {
        val multilineMessage = """
            First line
            Second line
            Third line
        """.trimIndent()
        commandData = commandData.copy(structuredMode = true, message = multilineMessage)

        val command = nativeStrategy.buildCommand(commandData)
        
        assertThat(command).contains("-m")
        assertThat(command.last()).isEqualTo("""
            SYSTEM Instead of making changes to the code, markdown files should be used to track progress on the feature.
            SYSTEM A plan consists of a detailed description of the requested feature and a separate file with a checklist for tracking the progress.
            SYSTEM The file should be saved in the .coding-aider-plans directory in the project.
            SYSTEM Always start plans with the line [Coding Aider Plan] and checklists with [Coding Aider Plan - Checklist] at the beginning of the file and use this marker in existing files to identify plans and checklists.
            SYSTEM If a separate checklist exists, it is referenced in the plan using markdown file references.
            SYSTEM Likewise the plan is referenced in the checklist using markdown file references. Be sure to use correct relative path (same folder) references between the files.
            SYSTEM Never proceed with changes if the plan is not committed yet.
            SYSTEM Once the plan properly describes the changes, start implementing them step by step. Commit each change as you go.
            SYSTEM No plan exists yet. Write a detailed description of the requested feature and the needed changes.
            SYSTEM Save the plan in a new markdown file with a suitable name in the .coding-aider-plans directory.
            SYSTEM Create a separate checklist file to track the progress of implementing the plan.
            SYSTEM Only proceed with changes after creating and committing the plan.
            [STRUCTURED MODE] First line
            Second line
            Third line""".trimIndent())
    }

    @Test
    fun `DockerAiderExecutionStrategy handles structured mode with multi-line message`() {
        val multilineMessage = """
            First line
            Second line
            Third line
        """.trimIndent()
        commandData = commandData.copy(structuredMode = true, message = multilineMessage)
        whenever(dockerManager.getCidFilePath()).thenReturn("/tmp/docker.cid")
        
        val command = dockerStrategy.buildCommand(commandData)
        
        assertThat(command).contains("-m")
        assertThat(command.last()).isEqualTo("""
            SYSTEM Instead of making changes to the code, markdown files should be used to track progress on the feature.
            SYSTEM A plan consists of a detailed description of the requested feature and a separate file with a checklist for tracking the progress.
            SYSTEM The file should be saved in the .coding-aider-plans directory in the project.
            SYSTEM Always start plans with the line [Coding Aider Plan] and checklists with [Coding Aider Plan - Checklist] at the beginning of the file and use this marker in existing files to identify plans and checklists.
            SYSTEM If a separate checklist exists, it is referenced in the plan using markdown file references.
            SYSTEM Likewise the plan is referenced in the checklist using markdown file references. Be sure to use correct relative path (same folder) references between the files.
            SYSTEM Never proceed with changes if the plan is not committed yet.
            SYSTEM Once the plan properly describes the changes, start implementing them step by step. Commit each change as you go.
            SYSTEM No plan exists yet. Write a detailed description of the requested feature and the needed changes.
            SYSTEM Save the plan in a new markdown file with a suitable name in the .coding-aider-plans directory.
            SYSTEM Create a separate checklist file to track the progress of implementing the plan.
            SYSTEM Only proceed with changes after creating and committing the plan.
            [STRUCTURED MODE] First line
            Second line
            Third line""".trimIndent())
    }
}
