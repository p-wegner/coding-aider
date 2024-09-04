package de.andrena.aidershortcut.utils

import com.intellij.openapi.project.Project

object GitUtils {
    fun getCurrentCommitHash(project: Project): String? {
        return GitUtil.getCurrentRevision(project)
    }

    // compares the current local file state with the given commit
    fun openGitComparisonTool(project: Project, commitHash: String) {
        GitCompareWithRevisionDialog.show(project, commitHash)
    }
}
