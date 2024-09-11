package de.andrena.codingaider.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.IDEBasedExecutor
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.FileTraversal
import org.yaml.snakeyaml.Yaml
import javax.swing.JComponent

class ApplyDesignPatternAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        applyDesignPattern(project, files)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabledAndVisible = project != null && !files.isNullOrEmpty()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    companion object {
        private fun applyDesignPattern(project: Project, virtualFiles: Array<VirtualFile>) {
            val patterns = loadDesignPatterns()
            val dialog = DesignPatternDialog(project, patterns.keys.toList())
            if (dialog.showAndGet()) {
                val selectedPattern = dialog.getSelectedPattern()
                val patternInfo = patterns[selectedPattern] ?: return

                val allFiles = FileTraversal.traverseFilesOrDirectories(virtualFiles)
                val fileNames = allFiles.map { it.filePath }

                val settings = AiderSettings.getInstance(project)
                val commandData = CommandData(
                    message = "Apply the ${selectedPattern.capitalize()} design pattern to the following files: $fileNames. " +
                            "Here's information about the pattern:\n" +
                            "Description: ${patternInfo["description"]}\n" +
                            "When to apply: ${patternInfo["when_to_apply"]}\n" +
                            "What it does: ${patternInfo["what_it_does"]}\n" +
                            "Benefits: ${patternInfo["benefits"]}\n" +
                            "Please refactor the code to implement this design pattern.",
                    useYesFlag = true,
                    llm = settings.llm,
                    additionalArgs = settings.additionalArgs,
                    files = allFiles,
                    isShellMode = false,
                    lintCmd = settings.lintCmd,
                    deactivateRepoMap = settings.deactivateRepoMap,
                    editFormat = settings.editFormat
                )
                IDEBasedExecutor(project, commandData).execute()
            }
        }

        private fun loadDesignPatterns(): Map<String, Map<String, String>> {
            val inputStream = ApplyDesignPatternAction::class.java.getResourceAsStream("/design_patterns.yaml")
            return Yaml().load(inputStream)
        }
    }

    private class DesignPatternDialog(project: Project, patterns: List<String>) : DialogWrapper(project) {
        private lateinit var patternComboBox: ComboBox<String>

        init {
            title = "Select Design Pattern"
            init()
        }

        override fun createCenterPanel(): JComponent {
            return panel {
                row("Select a design pattern:") {
                    patternComboBox = comboBox(patterns).component
                }
            }
        }

        fun getSelectedPattern(): String = patternComboBox.selectedItem as String
    }
}
