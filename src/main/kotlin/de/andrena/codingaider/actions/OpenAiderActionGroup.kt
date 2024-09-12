package de.andrena.codingaider.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.popup.JBPopupFactory

class OpenAiderActionGroup : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val actionManager = ActionManager.getInstance()
        val aiderActionGroup = actionManager.getAction("de.andrena.codingaider.AiderActionGroup") as? ActionGroup
            ?: return

        val flatActionGroup = DefaultActionGroup()
        addActionsRecursively(aiderActionGroup, flatActionGroup)

        val popup = JBPopupFactory.getInstance()
            .createActionGroupPopup(
                "Aider Actions",
                flatActionGroup,
                e.dataContext,
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                true
            )

        popup.showCenteredInCurrentWindow(project)
    }

    private fun addActionsRecursively(sourceGroup: ActionGroup, targetGroup: DefaultActionGroup) {
        for (action in sourceGroup.getChildren(null)) {
            when (action) {
                is ActionGroup -> addActionsRecursively(action, targetGroup)
                else -> targetGroup.add(action)
            }
        }
    }
}
