package de.andrena.codingaider.utils

import com.intellij.openapi.application.ApplicationManager.getApplication
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.actions.diff.ShowDiffAction
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
                getApplication().invokeLater {
                    val changes = getChangesSince(repository,commitHash)

                    ShowDiffAction.showDiffForChange(
                        project,
                        changes
                    )
                    afterAction()
                }
            }
        }
    }

    private fun getChangesSince(repository: GitRepository, commitHash: String): List<Change> {
        val vcsManager = repository.vcs.vcsHistoryProvider
        val root = repository.root
        val changes = mutableListOf<Change>()
        
        repository.currentBranch?.let { branch ->
            val gitVcs = repository.vcs
            
            val gitVcsRoot = gitVcs.findGitVcsRoot(root) ?: return@let
            val currentState = repository.currentRevision ?: return@let

            val diffProvider = gitVcs.diffProvider
            changes.addAll(diffProvider.compare(
                gitVcsRoot.path,
                commitHash,
                currentState
            ))
        }
        
        return changes
    }

    private fun getGitRepository(project: Project): GitRepository? {
        return GitUtil.getRepositoryManager(project).repositories.firstOrNull()
    }

    fun findGitRoot(directory: File): File? =
        generateSequence(directory) { it.parentFile }
            .find { File(it, ".git").exists() }
}
