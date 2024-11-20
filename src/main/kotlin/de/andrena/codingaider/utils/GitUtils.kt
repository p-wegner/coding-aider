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

    fun openGitComparisonTool(project: Project, commitHash: String, afterAction: () -> Unit) {
        getApplication().executeOnPooledThread<Unit> {
            val repository = getGitRepository(project)
            if (repository != null) {
                getApplication().runReadAction {
                    val changes = getChangesSince(repository, commitHash)
                    getApplication().invokeLater {
                        GitDiffPresenter.presentChanges(project, changes)
                        afterAction()
                    }
                }
            }
        }
    }

    private fun getChangesSince(repository: GitRepository, commitHash: String): List<Change> {
        return getApplication().runReadAction<List<Change>> {
            val root = repository.root
            val changes: Collection<Change>? = repository.currentBranch?.let { branch ->
                val gitVcs = repository.vcs
                val revNum = gitVcs.parseRevisionNumber(commitHash) ?: return@runReadAction emptyList()
                gitVcs.diffProvider.compareWithWorkingDir(root, revNum)
            }
            changes?.toList() ?: emptyList()
        }
    }

    private fun getGitRepository(project: Project): GitRepository? {
        return GitUtil.getRepositoryManager(project).repositories.firstOrNull()
    }

    fun findGitRoot(directory: File): File? =
        generateSequence(directory) { it.parentFile }
            .find { File(it, ".git").exists() }
}

