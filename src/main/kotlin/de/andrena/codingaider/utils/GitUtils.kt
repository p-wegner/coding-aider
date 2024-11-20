package de.andrena.codingaider.utils

import com.intellij.openapi.application.ApplicationManager.getApplication
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ContentRevision
import com.intellij.openapi.vcs.changes.CurrentContentRevision
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vcs.impl.SimpleContentRevision
import com.intellij.openapi.vfs.LocalFileSystem
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
                    val changes = getChanges(repository).map { file ->
                        val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file)
                        if (virtualFile != null) {
                            val beforeRevision = SimpleContentRevision(
                                "",
                                virtualFile.path,
                                VcsRevisionNumber.before()
                            )
                            val afterRevision = CurrentContentRevision(virtualFile)
                            Change(beforeRevision as ContentRevision, afterRevision)
                        } else null
                    }.filterNotNull()

                    com.intellij.openapi.vcs.changes.actions.ShowDiffAction.showDiffForChange(
                        project,
                        changes
                    )
                    afterAction()
                }
            }
        }
    }

    private fun getChanges(repository: GitRepository): List<File> {
        val root = repository.root
        return repository.changingFiles.map { 
            File(root.path, it)
        }
    }

    private fun getGitRepository(project: Project): GitRepository? {
        return GitUtil.getRepositoryManager(project).repositories.firstOrNull()
    }

    fun findGitRoot(directory: File): File? =
        generateSequence(directory) { it.parentFile }
            .find { File(it, ".git").exists() }
}
