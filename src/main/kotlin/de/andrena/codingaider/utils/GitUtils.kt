package de.andrena.codingaider.utils

import com.intellij.openapi.application.ApplicationManager.getApplication
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
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
                    val changesViewManager = com.intellij.openapi.vcs.changes.ui.ChangesViewManager.getInstance(project)
                    changesViewManager.selectAndShowChangesFromCommit(commitHash)
                    afterAction()
                }
            }
    }

    private fun getGitRepository(project: Project): GitRepository? {
        return GitUtil.getRepositoryManager(project).repositories.firstOrNull()
    }

    fun findGitRoot(directory: File): File? =
        generateSequence(directory) { it.parentFile }
            .find { File(it, ".git").exists() }
}
