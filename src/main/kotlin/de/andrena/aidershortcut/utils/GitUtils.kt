package de.andrena.aidershortcut.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitUtil
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import git4idea.ui.GitCompareWithRevisionDialog

object GitUtils {
    fun getCurrentCommitHash(project: Project): String? {
        val repository = getGitRepository(project)
        return repository?.currentRevision
    }

    fun openGitComparisonTool(project: Project, commitHash: String) {
        val repository = getGitRepository(project)
        if (repository != null) {
            val changedFiles = getChangedFiles(project)
            GitCompareWithRevisionDialog.show(project, changedFiles, repository.root, commitHash)
        }
    }

    private fun getGitRepository(project: Project): GitRepository? {
        return GitUtil.getRepositoryManager(project).repositories.firstOrNull()
    }

    private fun getChangedFiles(project: Project): List<VirtualFile> {
        val changeListManager = ChangeListManager.getInstance(project)
        return changeListManager.affectedFiles
    }
}
