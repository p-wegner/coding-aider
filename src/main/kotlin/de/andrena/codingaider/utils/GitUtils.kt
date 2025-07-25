package de.andrena.codingaider.utils

import com.intellij.openapi.application.ApplicationManager.getApplication
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import git4idea.GitUtil
import git4idea.repo.GitRepository
import java.io.File

object GitUtils {
    fun getCurrentCommitHash(project: Project): String? {
        return getApplication().executeOnPooledThread<String?> {
            val repository = getGitRepository(project)
            repository?.currentRevision
        }.get()
    }

    // TODO: why does this slow down the editor when the action is no even called?
    fun openGitComparisonTool(project: Project, commitHash: String, afterAction: () -> Unit) {
        getApplication().executeOnPooledThread<Unit> {
            val repository = getGitRepository(project)
            if (repository != null) {
                val changes = getChangesSince(repository, commitHash)
                getApplication().invokeLater {
                    GitDiffPresenter.presentChanges(project, changes)
                    afterAction()
                }
            }
        }
    }

    //
    private fun getChangesSince(repository: GitRepository, commitHash: String): List<Change> {
        val root = repository.root
        val gitVcs = repository.vcs
        val revNum = gitVcs.parseRevisionNumber(commitHash) ?: return emptyList()
        val changes: Collection<Change>? = gitVcs.diffProvider.compareWithWorkingDir(root, revNum)
        return changes?.toList() ?: emptyList()
    }

    private fun getGitRepository(project: Project): GitRepository? {
        return GitUtil.getRepositoryManager(project).repositories.firstOrNull()
    }

    fun findGitRoot(directory: File): File? =
        generateSequence(directory) { it.parentFile }
            .find { File(it, ".git").exists() }
}

