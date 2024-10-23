package de.andrena.codingaider.utils

import com.intellij.openapi.application.ApplicationManager.getApplication
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.actions.diff.ShowCombinedDiffAction
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
            val changes: List<Change> = if (repository != null) {
                getChanges(project, repository, commitHash)
            } else {
                emptyList()
            }
            getApplication().invokeLater {
                if (changes.isNotEmpty()) {
                    ShowCombinedDiffAction.showDiff(
                        project,
                        changes
                    )
                    afterAction()
                }
            }
        }
    }

    private fun getChanges(project: Project, repository: GitRepository, commitHash: String): List<Change> {
        val gitVcs = GitUtil.getRepositoryManager(project).getRepositoryForRoot(repository.root)?.vcs
        val root = repository.root
        return if (gitVcs != null) {
            val revision = gitVcs.parseRevisionNumber(commitHash) ?: return emptyList()
            gitVcs.diffProvider.compareWithWorkingDir(root, revision)?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun getGitRepository(project: Project): GitRepository? {
        return GitUtil.getRepositoryManager(project).repositories.firstOrNull()
    }

    fun findGitRoot(directory: File): File? =
        generateSequence(directory) { it.parentFile }
            .find { File(it, ".git").exists() }

}
