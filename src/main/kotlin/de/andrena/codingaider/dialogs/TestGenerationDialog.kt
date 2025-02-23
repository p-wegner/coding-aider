package de.andrena.codingaider.dialogs

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.settings.AiderProjectSettingsConfigurable
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.TestGenerationPromptService
import de.andrena.codingaider.settings.AiderProjectSettings
import de.andrena.codingaider.settings.AiderProjectSettings.TestTypeConfiguration
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.FileTraversal
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList

class TestGenerationDialog(
    private val project: Project,
    private val selectedFiles: Array<VirtualFile>
) : DialogWrapper(project) {
    private val settings = AiderProjectSettings.getInstance(project)
    private val settingsButton = createSettingsButton()
    private val testTypeComboBox = ComboBox<TestTypeConfiguration>().apply {
        renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (component is JLabel && value is TestTypeConfiguration) {
                    text = value.name
                }
                return component
            }
        }
        addActionListener {
            updatePromptTemplate()
        }
    }
    
    private val promptArea = JBTextArea().apply {
        rows = 15
        lineWrap = true
        wrapStyleWord = true
        font = font.deriveFont(12f)
        emptyText.text = """Enter additional instructions (optional):
        
- Specify test scenarios or edge cases
- Add specific testing requirements
- Include custom assertions or validations
- Note any special setup needed"""
    }

    init {
        title = "Generate Tests"
        init()
        updateTestTypes()
    }

    private fun updateTestTypes() {
        testTypeComboBox.removeAllItems()
        settings.getTestTypes()
            .filter { it.isEnabled }
            .forEach { testTypeComboBox.addItem(it) }
    }

    private fun updatePromptTemplate() {
        val selectedType = getSelectedTestType()
        if (selectedType != null) {
            promptArea.emptyText.text = selectedType.promptTemplate
        }
    }

    private fun createSettingsButton(): ActionButton {
        val settingsAction = object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, AiderProjectSettingsConfigurable::class.java)
            }
        }
        val presentation = Presentation("Open Settings").apply {
            icon = AllIcons.General.Settings
            description = "Open test type settings"
        }
        return ActionButton(
            settingsAction, presentation, "TestTypeSettingsButton", ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
        )
    }

    override fun createCenterPanel(): JComponent {
        val panel = panel {
            row("Test Type:") {
                cell(testTypeComboBox)
                    .resizableColumn()
                    .align(com.intellij.ui.dsl.builder.AlignX.FILL)
                cell(settingsButton)
                    .align(com.intellij.ui.dsl.builder.AlignX.RIGHT)
            }
            row("Instructions:") {
                cell(JBScrollPane(promptArea))
                    .resizableColumn()
                    .align(com.intellij.ui.dsl.builder.AlignY.FILL)
                    .align(com.intellij.ui.dsl.builder.AlignX.FILL)
            }.resizableRow()
        }
        
        panel.preferredSize = java.awt.Dimension(800, 600)
        panel.minimumSize = java.awt.Dimension(400, 300)
        return panel
    }

    override fun doValidate(): ValidationInfo? {
        val testType = getSelectedTestType()
        return when {
            testType == null -> ValidationInfo("Please select a test type")
            settings.getTestTypes().isEmpty() -> ValidationInfo("No test types configured. Please configure test types in Project Settings.")
            else -> null
        }
    }

    override fun doOKAction() {
        val testType = getSelectedTestType() ?: return
        val allFiles = FileTraversal.traverseFilesOrDirectories(selectedFiles)
        val settings = AiderSettings.getInstance()

        try {
            // Add context files to the file list
            val contextFiles = testType.contextFiles.map { FileData(it, false) }
        
            val commandData = CommandData(
                message = buildPrompt(testType, allFiles),
                useYesFlag = true,
                files = allFiles +  contextFiles,
                projectPath = project.basePath ?: "",
                llm = settings.llm,
                additionalArgs = settings.additionalArgs,
                lintCmd = settings.lintCmd,
                aiderMode = AiderMode.NORMAL,
                sidecarMode = settings.useSidecarMode,
            )

            super.doOKAction()
            IDEBasedExecutor(project, commandData).execute()
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                "Failed to generate tests: ${e.message}",
                "Test Generation Error"
            )
        }
    }

    private fun buildPrompt(testType: TestTypeConfiguration, files: List<FileData>): String {
        return project.service<TestGenerationPromptService>().buildPrompt(testType, files, getAdditionalPrompt())
    }

    private fun getSelectedTestType(): TestTypeConfiguration? = testTypeComboBox.selectedItem as? TestTypeConfiguration
    private fun getAdditionalPrompt(): String = promptArea.text
}
