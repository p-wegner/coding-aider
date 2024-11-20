package de.andrena.codingaider.utils

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.actions.diff.ShowDiffAction

class GitDiffPresenter {
    companion object {
        fun presentChangesSimple(project: Project, changes: List<Change>) {
            if (changes.isNotEmpty()) {
                val diffContentFactory = DiffContentFactory.getInstance()
                val requests = changes.map { change ->
                    val beforeContent = change.beforeRevision?.content?.let { diffContentFactory.create(project, it) }
                        ?: diffContentFactory.createEmpty()
                    val afterContent = change.afterRevision?.content?.let { diffContentFactory.create(project, it) }
                        ?: diffContentFactory.createEmpty()

                    SimpleDiffRequest(
                        "Changes",
                        beforeContent,
                        afterContent,
                        "Before",
                        "After"
                    )
                }

                requests.forEach { request ->
                    DiffManager.getInstance().showDiff(project, request)
                }
            }
            fun presentChanges(project: Project, changes: List<Change>) {
                ShowDiffAction.showDiffForChange(project, changes)
            }
        }
    }
}