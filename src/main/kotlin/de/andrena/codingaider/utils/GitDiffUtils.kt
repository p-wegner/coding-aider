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
    data class DiffResult(
        val files: List<FileData>,
        val diffContent: String
    )

    @Throws(VcsException::class)
    fun getChangedFiles(project: Project, baseCommit: String, targetCommit: String): DiffResult {
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
    ): DiffResult {
        try {
            // Get changed file names
            val nameHandler = GitLineHandler(repository.project, repository.root, GitCommand.DIFF)
            nameHandler.addParameters("--name-only", "--no-renames", baseCommit, targetCommit)
            
            // Get actual diff content
            val diffHandler = GitLineHandler(repository.project, repository.root, GitCommand.DIFF)
            diffHandler.addParameters(baseCommit, targetCommit)
            
            val nameResult = Git.getInstance().runCommand(nameHandler)
            val diffResult = Git.getInstance().runCommand(diffHandler)
            
            if (!nameResult.success() || !diffResult.success()) {
                throw VcsException("Failed to get changes between commits: ${nameResult.errorOutputAsJoinedString}")
            }
            // Get file paths and ensure they're normalized
            val changedFiles = nameResult.output
                .filter { it.isNotEmpty() }
                .map { 
                    val fullPath = repository.root.path + "/" + it
                    FileData(FileTraversal.normalizedFilePath(fullPath), false) 
                }
                .distinctBy { it.normalizedFilePath }

            if (changedFiles.isEmpty()) {
                throw VcsException("No changes found between $baseCommit and $targetCommit")
            }

            return DiffResult(
                files = changedFiles,
                diffContent = diffResult.output.joinToString("\n")
            )
        } catch (e: Exception) {
            throw VcsException("Failed to get changes between commits:\n${e.message}")
        }
    }
}
