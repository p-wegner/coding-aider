package de.andrena.codingaider.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.popup.JBPopupFactory

class OpenAiderActionGroup : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val actionManager = ActionManager.getInstance()

        val flatActionGroup = DefaultActionGroup()
        val aiderActionGroupId = "de.andrena.codingaider.AiderActionGroup"
        
        actionManager.getActionIdList(aiderActionGroupId).forEach { actionId ->
            val action = actionManager.getAction(actionId)
            if (action !is Separator) {
                flatActionGroup.add(action)
            }
        }

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
}
