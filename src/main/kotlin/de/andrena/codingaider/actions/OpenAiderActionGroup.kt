package de.andrena.codingaider.actions

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.popup.JBPopupFactory

class OpenAiderActionGroup : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val actionManager = ActionManager.getInstance()
        val aiderActionGroup = actionManager.getAction("de.andrena.codingaider.AiderActionGroup") as? ActionGroup
            ?: return

        val popup = JBPopupFactory.getInstance()
            .createActionGroupPopup(
                "Aider Actions",
                DefaultActionGroup(aiderActionGroup),
                e.dataContext,
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                true
            )

        popup.showCenteredInCurrentWindow(project)
    }
}
