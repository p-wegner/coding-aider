package de.andrena.codingaider.actions.git

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import git4idea.GitUtil
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import org.mockito.kotlin.*

class GitCodeReviewActionTest : BasePlatformTestCase() {
    private lateinit var action: GitCodeReviewAction
    private lateinit var event: AnActionEvent
    private lateinit var project: Project
    private lateinit var repositoryManager: GitRepositoryManager
    private lateinit var repository: GitRepository

    override fun setUp() {
        super.setUp()
        action = GitCodeReviewAction()
        project = mock()
        repositoryManager = mock()
        repository = mock()
        event = TestActionEvent()

        whenever(project.getService(GitRepositoryManager::class.java)).thenReturn(repositoryManager)
        whenever(repositoryManager.repositories).thenReturn(listOf(repository))
    }

    fun testActionPerformedWithNoGitRepository() {
        whenever(repositoryManager.repositories).thenReturn(emptyList())
        
        action.actionPerformed(event)
        
        // Should show error notification
        // Note: This would require mocking the notification system
    }

    fun testActionPerformedWithInvalidGitRefs() {
        val dialog = mock<GitCodeReviewDialog>()
        whenever(dialog.showAndGet()).thenReturn(true)
        whenever(dialog.getSelectedRefs()).thenReturn(Pair("invalid-ref", "main"))
        whenever(repository.git.runCommand(any())).thenThrow(VcsException("Invalid ref"))
        
        action.actionPerformed(event)
        
        // Should show error notification
    }

    fun testActionPerformedSuccessfully() {
        val dialog = mock<GitCodeReviewDialog>()
        whenever(dialog.showAndGet()).thenReturn(true)
        whenever(dialog.getSelectedRefs()).thenReturn(Pair("main", "feature"))
        whenever(dialog.getPrompt()).thenReturn("Review the changes")
        
        whenever(repository.git.runCommand(any())).thenReturn(mock())
        
        action.actionPerformed(event)
        
        // Verify command execution
        verify(repository.git).runCommand(any())
    }
}
