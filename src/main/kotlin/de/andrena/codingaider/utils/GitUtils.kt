package de.andrena.codingaider.utils

import com.intellij.openapi.application.ApplicationManager.getApplication
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentManager
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentProvider
import com.intellij.openapi.wm.ToolWindowManager
import git4idea.GitVcs
import git4idea.repo.GitRepositoryManager
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
                getChanges(project, repository.root, commitHash)
            } else {
                emptyList()
            }
            getApplication().invokeLater {
                if (changes.isNotEmpty()) {
                    // Open the Changes tool window
                    val toolWindow = ToolWindowManager.getInstance(project)
                        .getToolWindow(ChangesViewContentManager.TOOLWINDOW_ID)
                    toolWindow?.show {
                        // After the window is shown, trigger the diff view
                        ChangesViewContentManager.getInstance(project).selectContent("Local Changes")
                        afterAction()
                    }
                }
            }
        }
    }

    private fun getChanges(project: Project, root: File, commitHash: String): List<Change> {
        val gitVcs = GitVcs.getInstance(project)
        return if (gitVcs != null) {
            val revision = gitVcs.parseRevisionNumber(commitHash) ?: return emptyList()
            gitVcs.diffProvider.compareWithWorkingDir(root, revision)?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun getGitRepository(project: Project) =
        GitRepositoryManager.getInstance(project).repositories.firstOrNull()

    fun findGitRoot(directory: File): File? =
        generateSequence(directory) { it.parentFile }
            .find { File(it, ".git").exists() }
}
