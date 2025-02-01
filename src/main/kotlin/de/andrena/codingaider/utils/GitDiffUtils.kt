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
        
        val result = Git.getInstance().runCommand(handler)
        if (!result.success()) {
            throw VcsException("Invalid Git reference: $ref")
        }
    }

    private fun getChangedFilesFromRepository(
        repository: GitRepository,
        baseCommit: String,
        targetCommit: String
    ): List<FileData> {
        try {
            val handler = GitLineHandler(repository.project, repository.root, GitCommand.DIFF)
            handler.addParameters("--name-only", baseCommit, targetCommit)
            
            val result = Git.getInstance().runCommand(handler)
            if (!result.success()) {
                throw VcsException("Failed to get changes between commits")
            }

            val changedFiles = result.output.lines()
                .filter { it.isNotEmpty() }
                .map { FileData(repository.root.path + "/" + it, false) }

            if (changedFiles.isEmpty()) {
                throw VcsException("No changes found between $baseCommit and $targetCommit")
            }

            return changedFiles
        } catch (e: Exception) {
            throw VcsException("Failed to get changes between commits:\n${e.message}")
        }
    }
}
