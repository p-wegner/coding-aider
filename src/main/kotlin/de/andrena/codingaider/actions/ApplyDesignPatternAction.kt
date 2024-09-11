package de.andrena.codingaider.actions

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
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
        private val objectMapper = ObjectMapper(YAMLFactory()).registerModule(
            KotlinModule.Builder().build()
        )

        private fun applyDesignPattern(project: Project, virtualFiles: Array<VirtualFile>) {
            val patterns = loadDesignPatterns()
            val dialog = DesignPatternDialog(project, patterns.keys.toList())
            if (dialog.showAndGet()) {
                val selectedPattern = dialog.getSelectedPattern()
                val patternInfo = patterns[selectedPattern] ?: return

                val allFiles = FileTraversal.traverseFilesOrDirectories(virtualFiles)
                val fileNames = allFiles.map { it.filePath }

                val settings = AiderSettings.getInstance(project)
                val additionalInfo = dialog.getAdditionalInfo()
                val commandData = CommandData(
                    message = buildInstructionMessage(selectedPattern, patternInfo, fileNames, additionalInfo),
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

        private fun buildInstructionMessage(
            selectedPattern: String,
            patternInfo: Map<String, String>,
            fileNames: List<String>,
            additionalInfo: String
        ): String {
            val baseMessage = """
                Apply the ${selectedPattern.capitalize()} design pattern to the following files: $fileNames.
                Here's information about the pattern:
                Description: ${patternInfo["description"]}
                When to apply: ${patternInfo["when_to_apply"]}
                What it does: ${patternInfo["what_it_does"]}
                Benefits: ${patternInfo["benefits"]}
                Please refactor the code to implement this design pattern. Provide a detailed explanation of the changes made and how they implement the ${selectedPattern.capitalize()} pattern.
            """.trimIndent()

            return if (additionalInfo.isNotBlank()) {
                """
                $baseMessage
                
                Additional information provided by the user:
                $additionalInfo
                
                Please take this additional information into account when applying the design pattern.
                """.trimIndent()
            } else {
                baseMessage
            }
        }

        private fun loadDesignPatterns(): Map<String, Map<String, String>> {
            val inputStream = ApplyDesignPatternAction::class.java.getResourceAsStream("/design_patterns.yaml")
            return objectMapper.readValue(inputStream)
        }
    }

    private class DesignPatternDialog(project: Project, private val patterns: List<String>) : DialogWrapper(project) {
        private lateinit var patternComboBox: ComboBox<String>
        private lateinit var additionalInfoField: com.intellij.ui.components.JBTextField

        init {
            title = "Apply Design Pattern"
            init()
        }

        override fun createCenterPanel(): JComponent {
            return panel {
                row("Select a design pattern:") {
                    patternComboBox = comboBox(patterns).component
                }
                row("Additional information (optional):") {
                    additionalInfoField = textField()
                        .comment("Provide any specific requirements or context for applying the pattern")
                        .focused()
                        .component
                }
            }
        }

        fun getSelectedPattern(): String = patternComboBox.selectedItem as String
        fun getAdditionalInfo(): String = additionalInfoField.text
    }
}
