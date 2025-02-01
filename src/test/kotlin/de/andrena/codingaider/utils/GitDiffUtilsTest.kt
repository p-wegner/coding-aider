package de.andrena.codingaider.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ContentRevision
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import git4idea.GitUtil
import git4idea.commands.Git
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import org.mockito.kotlin.*

class GitDiffUtilsTest : BasePlatformTestCase() {
    private lateinit var project: Project
    private lateinit var repositoryManager: GitRepositoryManager
    private lateinit var repository: GitRepository
    private lateinit var git: Git

    override fun setUp() {
        super.setUp()
        project = mock()
        repositoryManager = mock()
        repository = mock()
        git = mock()

        whenever(project.getService(GitRepositoryManager::class.java)).thenReturn(repositoryManager)
        whenever(repositoryManager.repositories).thenReturn(listOf(repository))
        whenever(repository.git).thenReturn(git)
    }

    fun testGetChangedFilesWithInvalidRefs() {
        whenever(git.runCommand(any())).thenThrow(VcsException("Invalid ref"))
        
        try {
            GitDiffUtils.getChangedFiles(project, "invalid", "main")
            fail("Should throw VcsException")
        } catch (e: VcsException) {
            assertEquals("Invalid Git reference: invalid", e.message)
        }
    }

    fun testGetChangedFilesWithValidRefs() {
        val change = mock<Change>()
        val revision = mock<ContentRevision>()
        whenever(revision.file.path).thenReturn("/path/to/file")
        whenever(change.afterRevision).thenReturn(revision)
        
        whenever(git.runCommand(any())).thenReturn(mock())
        whenever(git.getChanges(eq(repository), any(), any(), any())).then {
            @Suppress("UNCHECKED_CAST")
            (it.arguments[3] as MutableList<Change>).add(change)
        }
        
        val files = GitDiffUtils.getChangedFiles(project, "main", "feature")
        assertEquals(1, files.size)
        assertEquals("/path/to/file", files[0].filePath)
    }

    fun testGetChangedFilesWithNoChanges() {
        whenever(git.runCommand(any())).thenReturn(mock())
        whenever(git.getChanges(eq(repository), any(), any(), any())).then {
            // Don't add any changes to the list
        }
        
        try {
            GitDiffUtils.getChangedFiles(project, "main", "feature")
            fail("Should throw VcsException")
        } catch (e: VcsException) {
            assertEquals("No changes found between main and feature", e.message)
        }
    }
}
