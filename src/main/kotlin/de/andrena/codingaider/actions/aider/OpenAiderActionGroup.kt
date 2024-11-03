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
        addQuickAccessAction(
            popupActionGroup,
            "de.andrena.codingaider.actions.aider.AiderAction",
            "Start Aider",
            AllIcons.Actions.Execute,
            'A'
        )
        addQuickAccessAction(
            popupActionGroup,
            "de.andrena.codingaider.actions.aider.AiderShellAction",
            "Start Aider in Shell Mode",
            AllIcons.Actions.StartDebugger,
            'M'
        )
        addQuickAccessAction(
            popupActionGroup,
            "de.andrena.codingaider.actions.aider.DocumentCodeAction",
            "Document",
            AllIcons.Actions.AddMulticaret,
            'D'
        )
        addQuickAccessAction(
            popupActionGroup,
            "de.andrena.codingaider.actions.aider.DocumentEachFolderAction",
            "Document Each Folder",
            AllIcons.Actions.Dump,
            'E'
        )
        addQuickAccessAction(
            popupActionGroup,
            "de.andrena.codingaider.actions.aider.RefactorToCleanCodeAction",
            "Refactor",
            AllIcons.Actions.GroupBy,
            'R'
        )
        addQuickAccessAction(
            popupActionGroup,
            "de.andrena.codingaider.actions.aider.ApplyDesignPatternAction",
            "Apply Design Pattern",
            AllIcons.Actions.Edit,
            'A'
        )
        addQuickAccessAction(
            popupActionGroup,
            "de.andrena.codingaider.CommitAction",
            "Commit",
            AllIcons.Actions.Commit,
            'C'
        )
        addQuickAccessAction(
            popupActionGroup,
            "de.andrena.codingaider.actions.ide.PersistentFilesAction",
            "Persistent Files",
            AllIcons.Actions.ListFiles,
            'P'
        )
        addQuickAccessAction(
            popupActionGroup,
            "de.andrena.codingaider.actions.aider.AiderWebCrawlAction",
            "Web Crawl",
            AllIcons.Actions.Download,
            'W'
        )
        addQuickAccessAction(
            popupActionGroup,
            "de.andrena.codingaider.actions.aider.AiderClipboardImageAction",
            "Clipboard Image",
            AllIcons.Actions.Copy,
            'I'
        )
        addQuickAccessAction(
            popupActionGroup,
            "de.andrena.codingaider.actions.ide.ShowLastCommandResultAction",
            "Show Last Command Result",
            AllIcons.Actions.Preview,
            'L'
        )
        addQuickAccessAction(
            popupActionGroup,
            "de.andrena.codingaider.SettingsAction",
            "Open Aider Settings",
            AllIcons.General.Settings,
            'S'
        )

        val popup = JBPopupFactory.getInstance()
            .createActionGroupPopup(
                null, // No title
                popupActionGroup,
                e.dataContext,
                false,
                true,
                true,
                null,
                0,
                null
            )

        val inputEvent = e.inputEvent
        if (inputEvent is MouseEvent) {
            val point = RelativePoint(inputEvent)
            popup.show(point)
        } else {
            popup.showCenteredInCurrentWindow(project)
        }
    }

    private fun addQuickAccessAction(
        group: DefaultActionGroup,
        actionId: String,
        text: String,
        icon: Icon,
        mnemonic: Char
    ) {
        val action = ActionManager.getInstance().getAction(actionId)
        action?.let {
            val textWithMnemonic = text.replaceFirst("$mnemonic", "&$mnemonic", false)
            val wrapper = object : AnAction(textWithMnemonic, it.templatePresentation.description, icon) {
                override fun actionPerformed(e: AnActionEvent) {
                    it.actionPerformed(e)
                }
            }
            group.add(wrapper)
        }
    }
}
