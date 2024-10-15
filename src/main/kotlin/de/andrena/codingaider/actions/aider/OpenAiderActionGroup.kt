package de.andrena.codingaider.actions.aider

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.event.MouseEvent
import javax.swing.Icon

class OpenAiderActionGroup : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val popupActionGroup = DefaultActionGroup()
        
        // Add quick access actions
        addQuickAccessAction(popupActionGroup, "de.andrena.codingaider.actions.aider.AiderAction", "Start Aider", AllIcons.Actions.Execute)
        addQuickAccessAction(popupActionGroup, "de.andrena.codingaider.actions.aider.AiderShellAction", "Start Aider in Shell Mode", AllIcons.Actions.StartDebugger)
        addQuickAccessAction(popupActionGroup, "de.andrena.codingaider.actions.aider.DocumentCodeAction", "Document", AllIcons.Actions.AddMulticaret)
        addQuickAccessAction(popupActionGroup, "de.andrena.codingaider.actions.aider.RefactorToCleanCodeAction", "Refactor", AllIcons.Actions.GroupBy)
        addQuickAccessAction(popupActionGroup, "de.andrena.codingaider.actions.aider.ApplyDesignPatternAction", "Apply Design Pattern", AllIcons.Actions.Edit)
        addQuickAccessAction(popupActionGroup, "de.andrena.codingaider.CommitAction", "Commit", AllIcons.Actions.Commit)
        addQuickAccessAction(popupActionGroup, "de.andrena.codingaider.actions.ide.PersistentFilesAction", "Persistent Files", AllIcons.Actions.ListFiles)
        addQuickAccessAction(popupActionGroup, "de.andrena.codingaider.actions.aider.AiderWebCrawlAction", "Web Crawl", AllIcons.Actions.Download)
        addQuickAccessAction(popupActionGroup, "de.andrena.codingaider.actions.aider.AiderClipboardImageAction", "Clipboard Image", AllIcons.Actions.Copy)
        addQuickAccessAction(popupActionGroup, "de.andrena.codingaider.actions.ide.ShowLastCommandResultAction", "Show Last Command Result", AllIcons.Actions.Preview)
        addQuickAccessAction(popupActionGroup, "de.andrena.codingaider.SettingsAction", "Open Aider Settings", AllIcons.General.Settings)

        val popup = JBPopupFactory.getInstance()
            .createActionGroupPopup(
                null, // No title
                popupActionGroup,
                e.dataContext,
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                true
            )

        val inputEvent = e.inputEvent
        if (inputEvent is MouseEvent) {
            val point = RelativePoint(inputEvent)
            popup.show(point)
        } else {
            popup.showCenteredInCurrentWindow(project)
        }
    }

    private fun addQuickAccessAction(group: DefaultActionGroup, actionId: String, text: String, icon: Icon) {
        val action = ActionManager.getInstance().getAction(actionId)
        action?.let {
            val wrapper = object : AnAction(text, it.templatePresentation.description, icon) {
                override fun actionPerformed(e: AnActionEvent) {
                    it.actionPerformed(e)
                }
            }
            group.add(wrapper)
        }
    }
}
