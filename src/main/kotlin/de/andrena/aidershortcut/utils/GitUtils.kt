package de.andrena.aidershortcut.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.actions.diff.ShowDiffAction
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitUtil
import git4idea.repo.GitRepository

object GitUtils {
    fun getCurrentCommitHash(project: Project): String? {
        return ApplicationManager.getApplication().executeOnPooledThread<String?> {
            val repository = getGitRepository(project)
            repository?.currentRevision
        }.get()
    }

    fun openGitComparisonTool(project: Project, commitHash: String) {
        ApplicationManager.getApplication().invokeLater {
            val changes = ApplicationManager.getApplication().executeOnPooledThread<List<Change>> {
                val repository = getGitRepository(project)
                if (repository != null) {
                    getChanges(project, repository, commitHash)
                } else {
                    emptyList()
                }
            }.get()

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
            gitVcs.diffProvider.compareWithWorkingDir(root, revision)?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun getGitRepository(project: Project): GitRepository? {
        return GitUtil.getRepositoryManager(project).repositories.firstOrNull()
    }

    private fun getChangedFiles(project: Project): List<VirtualFile> {
        return ApplicationManager.getApplication().executeOnPooledThread<List<VirtualFile>> {
            val changeListManager = com.intellij.openapi.vcs.changes.ChangeListManager.getInstance(project)
            changeListManager.affectedFiles
        }.get()
    }
}
