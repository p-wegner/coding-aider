package de.andrena.aidershortcut.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.actions.diff.ShowDiffAction
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitUtil
import git4idea.repo.GitRepository

object GitUtils {
    fun getCurrentCommitHash(project: Project): String? {
        val repository = getGitRepository(project)
        return repository?.currentRevision
    }

    fun openGitComparisonTool(project: Project, commitHash: String) {
        val repository = getGitRepository(project)
        if (repository != null) {
            val changes = getChanges(project, repository, commitHash)
            if (changes.isNotEmpty()) {
                ShowDiffAction.showDiffForChange(
                    project,
                    changes,
                )
            }
        }
    }

    private fun getChanges(project: Project, repository: GitRepository, commitHash: String): List<Change> {
        val gitVcs = GitUtil.getRepositoryManager(project).getRepositoryForRoot(repository.root)?.vcs
        val root = repository.root
        return if (gitVcs != null) {
            val revision = gitVcs.parseRevisionNumber(commitHash) ?: return emptyList()
            return gitVcs.diffProvider.compareWithWorkingDir(root, revision)?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun getGitRepository(project: Project): GitRepository? {
        return GitUtil.getRepositoryManager(project).repositories.firstOrNull()
    }

    private fun getChangedFiles(project: Project): List<VirtualFile> {
        val changeListManager = com.intellij.openapi.vcs.changes.ChangeListManager.getInstance(project)
        return changeListManager.affectedFiles
    }
}
