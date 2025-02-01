package de.andrena.codingaider.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.Change
import git4idea.GitUtil
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import de.andrena.codingaider.command.FileData

object GitDiffUtils {
    @Throws(VcsException::class)
    fun getChangedFiles(project: Project, baseCommit: String, targetCommit: String): List<FileData> {
        if (baseCommit.isBlank() || targetCommit.isBlank()) {
            throw VcsException("Base commit and target commit must not be empty")
        }

        val repository = GitUtil.getRepositoryManager(project).repositories.firstOrNull()
            ?: throw VcsException("No Git repository found in project")

        validateGitRef(repository, baseCommit)
        validateGitRef(repository, targetCommit)

        return getChangedFilesFromRepository(repository, baseCommit, targetCommit)
    }

    private fun validateGitRef(repository: GitRepository, ref: String) {
        val handler = GitLineHandler(repository.project, repository.root, GitCommand.REV_PARSE)
        handler.addParameters("--verify", ref)
        
        val result = repository.git.runCommand(handler)
        if (result.exitCode != 0) {
            throw VcsException("Invalid Git reference: $ref")
        }
    }

    private fun getChangedFilesFromRepository(
        repository: GitRepository,
        baseCommit: String,
        targetCommit: String
    ): List<FileData> {
        try {
            val git = repository.git
            val changes = mutableListOf<Change>()

            git.getChanges(repository, baseCommit, targetCommit, changes)

            if (changes.isEmpty()) {
                throw VcsException("No changes found between $baseCommit and $targetCommit")
            }

            return changes.mapNotNull { change ->
                val file = change.afterRevision?.file ?: change.beforeRevision?.file
                file?.path?.let { FileData(it, false) }
            }
        } catch (e: Exception) {
            throw VcsException("Failed to get changes: ${e.message}")
        }
    }
}
