package de.andrena.aidershortcut.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitUtil
import git4idea.repo.GitRepository
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.wm.ToolWindowManager
import git4idea.GitVcs
import git4idea.changes.GitChangeUtils
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentManager
import com.intellij.openapi.vcs.changes.LocalChangeList

object GitUtils {
    fun getCurrentCommitHash(project: Project): String? {
        val repository = getGitRepository(project)
        return repository?.currentRevision
    }

    fun openGitComparisonTool(project: Project, commitHash: String) {
        val repository = getGitRepository(project)
        if (repository != null) {
            try {
                val gitVcs = GitVcs.getInstance(project)
                val currentBranch = repository.currentBranch
                val changes = if (currentBranch != null) {
                    GitChangeUtils.getDiffWithWorkingDir(
                        project,
                        repository.root,
                        commitHash,
                        null,
                        false
                    )
                } else {
                    emptyList()
                }

                val changeListManager = ChangeListManager.getInstance(project)
                val changeList = LocalChangeList.createEmptyChangeList(project, "Changes since $commitHash")
                changeList.addChanges(changes)

                val changesViewContentManager = ChangesViewContentManager.getInstance(project)
                changesViewContentManager.selectContent("Local Changes")

                val toolWindowManager = ToolWindowManager.getInstance(project)
                val changesViewToolWindow = toolWindowManager.getToolWindow("Version Control")
                changesViewToolWindow?.show()
            } catch (e: VcsException) {
                // Handle exception (e.g., log it or show an error message)
            }
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
