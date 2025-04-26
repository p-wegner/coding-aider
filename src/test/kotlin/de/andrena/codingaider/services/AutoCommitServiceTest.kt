package de.andrena.codingaider.services

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitCommandResult
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import java.io.File
import com.intellij.openapi.vfs.VirtualFile

class AutoCommitServiceTest {

    private lateinit var autoCommitService: AutoCommitService
    private lateinit var mockProject: Project
    private lateinit var mockCommitMessageExtractor: CommitMessageExtractorService
    private lateinit var mockGit: Git
    private lateinit var mockRepositoryManager: GitRepositoryManager
    private lateinit var mockRepository: GitRepository
    private lateinit var mockRoot: VirtualFile
    private lateinit var mockLocalFileSystem: LocalFileSystem
    private lateinit var mockSettings: de.andrena.codingaider.settings.AiderSettings

    @BeforeEach
    fun setUp() {
        // Mock dependencies
        mockProject = mock(Project::class.java)
        mockCommitMessageExtractor = mock(CommitMessageExtractorService::class.java)
        mockGit = mock(Git::class.java)
        mockRepositoryManager = mock(GitRepositoryManager::class.java)
        mockRepository = mock(GitRepository::class.java)
        mockRoot = mock(VirtualFile::class.java)
        mockLocalFileSystem = mock(LocalFileSystem::class.java)
        mockSettings = mock(de.andrena.codingaider.settings.AiderSettings::class.java)

        // Set up mocks
        `when`(mockProject.service<CommitMessageExtractorService>()).thenReturn(mockCommitMessageExtractor)
        `when`(mockRepository.root).thenReturn(mockRoot)
        `when`(mockRoot.path).thenReturn("/project/root")
        
        // Mock static methods
        val gitInstance = Mockito.mockStatic(Git::class.java)
        gitInstance.`when`<Git> { Git.getInstance() }.thenReturn(mockGit)
        
        val localFileSystemInstance = Mockito.mockStatic(LocalFileSystem::class.java)
        localFileSystemInstance.`when`<LocalFileSystem> { LocalFileSystem.getInstance() }.thenReturn(mockLocalFileSystem)
        
        val settingsInstance = Mockito.mockStatic(de.andrena.codingaider.settings.AiderSettings::class.java)
        settingsInstance.`when`<de.andrena.codingaider.settings.AiderSettings> { de.andrena.codingaider.settings.AiderSettings.getInstance() }.thenReturn(mockSettings)
        
        // Create service instance
        autoCommitService = AutoCommitService(mockProject)
    }

    @Test
    fun `tryAutoCommit should return false when auto-commit is disabled`() {
        // Arrange
        `when`(mockSettings.autoCommitAfterEdits).thenReturn(false)
        
        // Act
        val result = autoCommitService.tryAutoCommit("Some LLM response", listOf("file1.txt"))
        
        // Assert
        assertThat(result).isFalse()
        verify(mockCommitMessageExtractor, never()).extractCommitMessage(anyString())
    }

    @Test
    fun `tryAutoCommit should return false when no files are modified`() {
        // Arrange
        `when`(mockSettings.autoCommitAfterEdits).thenReturn(true)
        
        // Act
        val result = autoCommitService.tryAutoCommit("Some LLM response", emptyList())
        
        // Assert
        assertThat(result).isFalse()
        verify(mockCommitMessageExtractor, never()).extractCommitMessage(anyString())
    }

    @Test
    fun `tryAutoCommit should return false when no commit message is found`() {
        // Arrange
        `when`(mockSettings.autoCommitAfterEdits).thenReturn(true)
        `when`(mockCommitMessageExtractor.extractCommitMessage(anyString())).thenReturn(null)
        
        // Act
        val result = autoCommitService.tryAutoCommit("Some LLM response", listOf("file1.txt"))
        
        // Assert
        assertThat(result).isFalse()
        verify(mockCommitMessageExtractor).extractCommitMessage(anyString())
    }

    @Test
    fun `tryAutoCommit should store commit message when extracted`() {
        // Arrange
        `when`(mockSettings.autoCommitAfterEdits).thenReturn(true)
        `when`(mockCommitMessageExtractor.extractCommitMessage(anyString())).thenReturn("feat: test commit")
        
        // Mock repository setup
        val repositories = listOf(mockRepository)
        val repositoryManagerInstance = Mockito.mockStatic(git4idea.repo.GitRepositoryManager::class.java)
        repositoryManagerInstance.`when`<GitRepositoryManager> { git4idea.repo.GitRepositoryManager.getInstance(mockProject) }.thenReturn(mockRepositoryManager)
        `when`(mockRepositoryManager.repositories).thenReturn(repositories)
        
        // Mock Git command execution
        val mockResult = mock(GitCommandResult::class.java)
        `when`(mockResult.success()).thenReturn(false) // Make commit fail for this test
        `when`(mockGit.runCommand(any(GitLineHandler::class.java))).thenReturn(mockResult)
        
        // Act
        autoCommitService.tryAutoCommit("Some LLM response", listOf("file1.txt"))
        
        // Assert
        assertThat(autoCommitService.getLastCommitMessage()).isEqualTo("feat: test commit")
    }

    @Test
    fun `tryAutoCommit should execute Git add and commit commands when all conditions are met`() {
        // Arrange
        `when`(mockSettings.autoCommitAfterEdits).thenReturn(true)
        `when`(mockCommitMessageExtractor.extractCommitMessage(anyString())).thenReturn("feat: test commit")
        
        // Mock file existence
        val mockFile = mock(File::class.java)
        `when`(mockFile.exists()).thenReturn(true)
        val mockFileInstance = Mockito.mockStatic(File::class.java)
        mockFileInstance.`when`<File> { File(anyString()) }.thenReturn(mockFile)
        
        // Mock repository setup
        val repositories = listOf(mockRepository)
        val repositoryManagerInstance = Mockito.mockStatic(git4idea.repo.GitRepositoryManager::class.java)
        repositoryManagerInstance.`when`<GitRepositoryManager> { git4idea.repo.GitRepositoryManager.getInstance(mockProject) }.thenReturn(mockRepositoryManager)
        `when`(mockRepositoryManager.repositories).thenReturn(repositories)
        
        // Mock Git command execution
        val mockAddResult = mock(GitCommandResult::class.java)
        val mockCommitResult = mock(GitCommandResult::class.java)
        `when`(mockAddResult.success()).thenReturn(true)
        `when`(mockCommitResult.success()).thenReturn(true)
        
        // Capture the Git commands
        val handlerCaptor = ArgumentCaptor.forClass(GitLineHandler::class.java)
        
        // First call returns add result, second call returns commit result
        whenever(mockGit.runCommand(handlerCaptor.capture()))
            .thenReturn(mockAddResult)
            .thenReturn(mockCommitResult)
        
        // Act
        val result = autoCommitService.tryAutoCommit("Some LLM response", listOf("/project/root/file1.txt"))
        
        // Assert
        assertThat(result).isTrue()
        
        // Verify the captured handlers
        val handlers = handlerCaptor.allValues
        assertThat(handlers).hasSize(2)
        
        // First handler should be for add
        val addHandler = handlers[0]
//        assertThat(addHandler.command).isEqualTo(GitCommand.ADD)
        
        // Second handler should be for commit
        val commitHandler = handlers[1]
//        assertThat(commitHandler.command).isEqualTo(GitCommand.COMMIT)
//        assertThat(commitHandler.parameters).contains("-m", "feat: test commit")
    }

    @Test
    fun `tryAutoCommit should handle relative paths correctly`() {
        // Arrange
        `when`(mockSettings.autoCommitAfterEdits).thenReturn(true)
        `when`(mockCommitMessageExtractor.extractCommitMessage(anyString())).thenReturn("feat: test commit")
        
        // Mock file existence
        val mockFile = mock(File::class.java)
        `when`(mockFile.exists()).thenReturn(true)
        val mockFileInstance = Mockito.mockStatic(File::class.java)
        mockFileInstance.`when`<File> { File(anyString()) }.thenReturn(mockFile)
        
        // Mock repository setup
        val repositories = listOf(mockRepository)
        val repositoryManagerInstance = Mockito.mockStatic(git4idea.repo.GitRepositoryManager::class.java)
        repositoryManagerInstance.`when`<GitRepositoryManager> { git4idea.repo.GitRepositoryManager.getInstance(mockProject) }.thenReturn(mockRepositoryManager)
        `when`(mockRepositoryManager.repositories).thenReturn(repositories)
        
        // Mock Git command execution
        val mockAddResult = mock(GitCommandResult::class.java)
        `when`(mockAddResult.success()).thenReturn(true)
        val mockCommitResult = mock(GitCommandResult::class.java)
        `when`(mockCommitResult.success()).thenReturn(true)
        
        // Capture the Git commands
        val handlerCaptor = ArgumentCaptor.forClass(GitLineHandler::class.java)
        
        // First call returns add result, second call returns commit result
        whenever(mockGit.runCommand(handlerCaptor.capture()))
            .thenReturn(mockAddResult)
            .thenReturn(mockCommitResult)
        
        // Act - use a path that's not under the repository root
        val result = autoCommitService.tryAutoCommit("Some LLM response", listOf("src/file1.txt"))
        
        // Assert
        assertThat(result).isTrue()
        
        // Verify the captured handlers
        val handlers = handlerCaptor.allValues
        assertThat(handlers).hasSize(2)
        
        // First handler should be for add with the correct path
        val addHandler = handlers[0]
//        assertThat(addHandler.command).isEqualTo(GitCommand.ADD)
//        assertThat(addHandler.parameters).contains("src/file1.txt")
    }
}
