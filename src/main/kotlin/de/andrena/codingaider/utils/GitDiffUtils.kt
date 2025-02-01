package de.andrena.codingaider.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import git4idea.GitUtil
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import de.andrena.codingaider.command.FileData
import git4idea.commands.Git

object GitDiffUtils {
    @Throws(VcsException::class)
    fun getChangedFiles(project: Project, baseCommit: String, targetCommit: String): List<FileData> {
        if (baseCommit.isBlank() || targetCommit.isBlank()) {
            throw VcsException("Base commit and target commit must not be empty")
        }

        val repository = GitUtil.getRepositoryManager(project).repositories.firstOrNull()
            ?: throw VcsException("No Git repository found in project")

        return getChangedFilesFromRepository(repository, baseCommit, targetCommit)
    }

    private fun getChangedFilesFromRepository(
        repository: GitRepository,
        baseCommit: String,
        targetCommit: String
    ): List<FileData> {
        try {
            val handler = GitLineHandler(repository.project, repository.root, GitCommand.DIFF)
            handler.addParameters("--name-only", "--no-renames", baseCommit, targetCommit)
            
            val result = Git.getInstance().runCommand(handler)
            if (!result.success()) {
                throw VcsException("Failed to get changes between commits: ${result.errorOutputAsJoinedString}")
            }

            // Get file paths and ensure they're normalized
            val changedFiles = result.output
                .filter { it.isNotEmpty() }
                .map { 
                    val fullPath = repository.root.path + "/" + it
                    FileData(FileTraversal.normalizedFilePath(fullPath), false) 
                }
                .distinctBy { it.normalizedFilePath }

            if (changedFiles.isEmpty()) {
                throw VcsException("No changes found between $baseCommit and $targetCommit")
            }

            return changedFiles
        } catch (e: Exception) {
            throw VcsException("Failed to get changes between commits:\n${e.message}")
        }
    }
}
