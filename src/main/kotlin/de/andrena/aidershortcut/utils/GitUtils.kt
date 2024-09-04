package de.andrena.aidershortcut.utils

import com.intellij.openapi.project.Project
import git4idea.GitUtil
import git4idea.ui.GitCompareWithRevisionDialog

object GitUtils {
    fun getCurrentCommitHash(project: Project): String? {
        return GitUtil.getCurrentRevision(project)
    }

    fun openGitComparisonTool(project: Project, commitHash: String) {
        GitCompareWithRevisionDialog.show(project, commitHash)
    }
}
