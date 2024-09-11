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
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.IDEBasedExecutor
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.FileTraversal
import java.awt.BorderLayout
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.plaf.basic.BasicComboBoxRenderer

fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

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
                Analyze the following files: $fileNames.
                Consider applying the ${selectedPattern.capitalize()} design pattern. Here's information about the pattern:
                Description: ${patternInfo["description"]}
                When to apply: ${patternInfo["when_to_apply"]}
                What it does: ${patternInfo["what_it_does"]}
                Benefits: ${patternInfo["benefits"]}
                
                Please follow these steps:
                1. Evaluate if applying the ${selectedPattern.capitalize()} pattern is appropriate for the given code.
                2. If it's appropriate:
                   a. Refactor the code to implement this design pattern.
                   b. Provide a detailed explanation of the changes made and how they implement the ${selectedPattern.capitalize()} pattern.
                3. If it's not appropriate:
                   a. Do not make any changes to the code.
                   b. Provide a detailed report explaining why the pattern is not applicable in this case.
                
                In both cases, justify your decision with specific references to the code and the pattern's characteristics.
            """.trimIndent()

            return if (additionalInfo.isNotBlank()) {
                """
                $baseMessage
                
                Important additional information to consider:
                $additionalInfo
                
                Please take this additional information into account when analyzing the applicability of the design pattern applying it.
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
        private val patternsInfo = Companion.loadDesignPatterns()
        private val patternComboBox: JComboBox<String> = ComboBox(patterns.map { pattern ->
            patternsInfo[pattern]?.get("display_title") ?: pattern
        }.toTypedArray()).apply {
            renderer = PatternRenderer()
            ToolTipManager.sharedInstance().dismissDelay = Integer.MAX_VALUE
        }
        private val additionalInfoArea = JTextArea(5, 50)

        init {
            title = "Apply Design Pattern"
            init()
        }

        override fun createCenterPanel(): JComponent {
            val panel = JPanel(BorderLayout())
            panel.border = JBUI.Borders.empty(10)

            val topPanel = JPanel(GridBagLayout())
            val gbc = GridBagConstraints().apply {
                fill = GridBagConstraints.HORIZONTAL
                insets = JBUI.insets(5)
                weightx = 1.0
                weighty = 0.0
                gridx = 0
                gridy = 0
            }

            val selectPatternLabel = JLabel("Select a design pattern:").apply {
                displayedMnemonic = KeyEvent.VK_S
                labelFor = patternComboBox
            }
            topPanel.add(selectPatternLabel, gbc)

            gbc.gridy++
            topPanel.add(patternComboBox, gbc)

            gbc.gridy++
            val messageLabel = JLabel("Additional information (optional):").apply {
                displayedMnemonic = KeyEvent.VK_A
                labelFor = additionalInfoArea
            }
            topPanel.add(messageLabel, gbc)

            gbc.gridy++
            gbc.weighty = 1.0
            gbc.fill = GridBagConstraints.BOTH
            additionalInfoArea.lineWrap = true
            additionalInfoArea.wrapStyleWord = true
            topPanel.add(JBScrollPane(additionalInfoArea), gbc)

            panel.add(topPanel, BorderLayout.CENTER)

            return panel
        }

        override fun createActions(): Array<Action> {
            val actions = super.createActions()
            (actions[0] as? DialogWrapperAction)?.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_O)
            (actions[1] as? DialogWrapperAction)?.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C)
            return actions
        }

        override fun show() {
            super.show()
            SwingUtilities.invokeLater {
                additionalInfoArea.requestFocusInWindow()
            }
        }

        fun getSelectedPattern(): String =
            patterns.getOrNull(patternComboBox.selectedIndex) ?: patterns.firstOrNull() ?: ""

        fun getAdditionalInfo(): String = additionalInfoArea.text

        private inner class PatternRenderer : BasicComboBoxRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is String) {
                    text = value
                    toolTipText = createTooltipText(patterns.getOrNull(index) ?: "")
                }
                return this
            }

            private fun createTooltipText(pattern: String): String {
                val info = patternsInfo[pattern] ?: return "No information available"
                return """
                    <html>
                    <b>Description:</b> ${info["description"]}<br><br>
                    <b>When to apply:</b> ${info["when_to_apply"]}<br><br>
                    <b>What it does:</b> ${info["what_it_does"]}<br><br>
                    <b>Benefits:</b> ${info["benefits"]}
                    </html>
                """.trimIndent()
            }
        }
    }
}
