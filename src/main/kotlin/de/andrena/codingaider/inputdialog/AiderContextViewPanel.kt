package de.andrena.codingaider.inputdialog

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.LayeredIcon
import com.intellij.util.ui.JBUI
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.services.AiderIgnoreService
import com.intellij.openapi.ui.Messages
import java.awt.BorderLayout
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.KeyStroke

class AiderContextViewPanel(
    private val project: Project,
    private val aiderContextView: AiderContextView
) : JPanel(BorderLayout()) {

    init {
        val fileActionGroup = createFileActionGroup()
        val fileStatusActionGroup = createFileStatusActionGroup()
        val combinedActionGroup = DefaultActionGroup().apply {
            addAll(fileActionGroup)
            addSeparator()
            addAll(fileStatusActionGroup)
        }

        val toolbar =
            ActionManager.getInstance().createActionToolbar("AiderContextToolbar", combinedActionGroup, true).apply {
                targetComponent = aiderContextView
                component.border = JBUI.Borders.empty(2, 2, 0, 2)
            }

        add(toolbar.component, BorderLayout.NORTH)
        add(aiderContextView.apply {
            border = JBUI.Borders.empty(0, 2, 2, 2)
        }, BorderLayout.CENTER)
    }

    private fun createFileActionGroup() = DefaultActionGroup().apply {
        add(object : AnAction("Manage .aiderignore", "Manage .aiderignore file", AllIcons.Actions.Properties) {
            override fun actionPerformed(e: AnActionEvent) {
                manageAiderIgnore()
            }

            override fun getActionUpdateThread() = ActionUpdateThread.BGT
            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = true
            }
        })
        add(object : AnAction("Add to .aiderignore", "Add selected files to .aiderignore", AllIcons.Actions.ToggleVisibility) {
            override fun actionPerformed(e: AnActionEvent) {
                addSelectedFilesToAiderIgnore()
            }

            override fun getActionUpdateThread() = ActionUpdateThread.BGT
            override fun update(e: AnActionEvent) {
                val selectedFiles = aiderContextView.getSelectedFiles()
                e.presentation.isEnabled = selectedFiles.isNotEmpty()
            }
        })
        add(object : AnAction("Add Files", "Add files to persistent files", LayeredIcon.ADD_WITH_DROPDOWN) {
            override fun actionPerformed(e: AnActionEvent) {
                val popup = JPopupMenu()
                popup.add(JMenuItem("From Project").apply {
                    addActionListener {
                        addFilesToContext()
                    }
                })
                popup.add(JMenuItem("Add Open Files").apply {
                    addActionListener {
                        aiderContextView.addOpenFilesToContext()
                    }
                })
                popup.add(JMenuItem("Add Plan Context Files").apply {
                    addActionListener {
                        aiderContextView.addPlanContextFilesToContext()
                    }
                })

                val component = e.inputEvent?.component
                popup.show(component, 0, component?.height ?: 0)
            }

            override fun getActionUpdateThread() = ActionUpdateThread.BGT
            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = true
                e.presentation.text = "Add Files"
            }
        }.also {
            it.registerCustomShortcutSet(
                CustomShortcutSet(
                    KeyStroke.getKeyStroke(
                        KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK
                    )
                ), aiderContextView
            )
        })

        add(object : AnAction(
            "Remove Files", "Remove selected files from the context view", AllIcons.Actions.Cancel
        ) {
            override fun actionPerformed(e: AnActionEvent) {
                aiderContextView.removeSelectedFiles()
            }

            override fun getActionUpdateThread() = ActionUpdateThread.BGT
            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = true
                e.presentation.text = "Remove Files (Del)"
            }
        })
    }

    private fun createFileStatusActionGroup() = DefaultActionGroup().apply {
        add(object : AnAction(
            "Toggle Read-Only Mode", "Toggle Read-Only Mode for selected file", AllIcons.Actions.Edit
        ) {
            override fun actionPerformed(e: AnActionEvent) {
                aiderContextView.toggleReadOnlyMode()
            }

            override fun getActionUpdateThread() = ActionUpdateThread.BGT
            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = true
                e.presentation.text = "Toggle Read-Only"
            }
        }.also {
            it.registerCustomShortcutSet(
                CustomShortcutSet(
                    KeyStroke.getKeyStroke(
                        KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK
                    )
                ), aiderContextView
            )
        })

        add(object : AnAction(
            "Toggle Persistent Files", "Toggle selected files' persistent status", AllIcons.Actions.MenuSaveall
        ) {
            override fun actionPerformed(e: AnActionEvent) {
                aiderContextView.togglePersistentFile()
            }

            override fun getActionUpdateThread() = ActionUpdateThread.BGT
            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = true
                e.presentation.text = "Toggle Persistent"
            }
        }.also {
            it.registerCustomShortcutSet(
                CustomShortcutSet(
                    KeyStroke.getKeyStroke(
                        KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK
                    )
                ), aiderContextView
            )
        })

        add(object : AnAction(
            "Stash Files", "Stash selected files", AllIcons.Vcs.ShelveSilent
        ) {
            override fun actionPerformed(e: AnActionEvent) {
                aiderContextView.stashSelectedFiles()
            }

            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = aiderContextView.getSelectedFiles().isNotEmpty()
            }

            override fun getActionUpdateThread() = ActionUpdateThread.BGT
        })
    }

    private fun addFilesToContext() {
        val fileChooser = javax.swing.JFileChooser().apply {
            currentDirectory = java.io.File(project.basePath!!)
            fileSelectionMode = javax.swing.JFileChooser.FILES_AND_DIRECTORIES
            isMultiSelectionEnabled = true
        }

        if (fileChooser.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            val selectedFiles = fileChooser.selectedFiles
            val aiderIgnoreService = project.service<AiderIgnoreService>()
            
            val fileDataList = selectedFiles.flatMap { file ->
                if (file.isDirectory) {
                    file.walkTopDown()
                        .filter { it.isFile && !aiderIgnoreService.isIgnored(it.absolutePath) }
                        .map { FileData(it.absolutePath, false) }
                        .toList()
                } else {
                    if (!aiderIgnoreService.isIgnored(file.absolutePath)) {
                        listOf(FileData(file.absolutePath, false))
                    } else {
                        emptyList()
                    }
                }
            }
            aiderContextView.addFilesToContext(fileDataList)
        }
    }
    
    private fun manageAiderIgnore() {
        val aiderIgnoreService = project.service<AiderIgnoreService>()
        val ignoreFile = aiderIgnoreService.createIgnoreFileIfNeeded()
        
        // Open the .aiderignore file in the editor
        com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openFile(ignoreFile, true)
    }
    
    private fun addSelectedFilesToAiderIgnore() {
        val selectedFiles = aiderContextView.getSelectedFiles()
        if (selectedFiles.isEmpty()) return
        
        val aiderIgnoreService = project.service<AiderIgnoreService>()
        aiderIgnoreService.createIgnoreFileIfNeeded()
        
        val projectPath = project.basePath ?: return
        
        for (file in selectedFiles) {
            val relativePath = getRelativePath(projectPath, file.filePath)
            val pattern = if (file.filePath.endsWith("/") || java.io.File(file.filePath).isDirectory) {
                "$relativePath/"
            } else {
                relativePath
            }
            
            aiderIgnoreService.addPatternToIgnoreFile(pattern)
        }
        
        // Remove the ignored files from the context view
        aiderContextView.removeSelectedFiles()
        
        com.intellij.openapi.ui.Messages.showInfoMessage(
            project,
            "Added ${selectedFiles.size} pattern(s) to .aiderignore file",
            "AiderIgnore Updated"
        )
    }
    
    private fun getRelativePath(projectPath: String, filePath: String): String {
        return if (filePath.startsWith(projectPath)) {
            filePath.substring(projectPath.length + 1).replace('\\', '/')
        } else {
            filePath.replace('\\', '/')
        }
    }
}
