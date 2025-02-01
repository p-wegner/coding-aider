package de.andrena.codingaider.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import git4idea.GitUtil
import git4idea.repo.GitRepository
import de.andrena.codingaider.command.FileData

object GitDiffUtils {
    fun getChangedFiles(project: Project, baseCommit: String, targetCommit: String): List<FileData> {
        val repository = GitUtil.getRepositoryManager(project).repositories.firstOrNull()
            ?: return emptyList()

        return getChangedFilesFromRepository(repository, baseCommit, targetCommit)
    }

    private fun getChangedFilesFromRepository(
        repository: GitRepository,
        baseCommit: String,
        targetCommit: String
    ): List<FileData> {
        val git = repository.git
        val changes = mutableListOf<Change>()

        git.getChanges(repository, baseCommit, targetCommit, changes)

        return changes.mapNotNull { change ->
            val file = change.afterRevision?.file ?: change.beforeRevision?.file
            file?.path?.let { FileData(it, false) }
        }
    }
}
