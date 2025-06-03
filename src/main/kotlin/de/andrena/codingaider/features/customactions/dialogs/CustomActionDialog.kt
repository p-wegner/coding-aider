package de.andrena.codingaider.features.customactions.dialogs

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
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.settings.AiderProjectSettingsConfigurable
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.features.customactions.CustomActionPromptService
import de.andrena.codingaider.settings.AiderProjectSettings
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.features.customactions.CustomActionConfiguration
import de.andrena.codingaider.utils.FileTraversal
import java.awt.Component
import java.awt.Dimension
import javax.swing.DefaultListCellRenderer
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList

class CustomActionDialog(
    private val project: Project,
    private val selectedFiles: Array<VirtualFile>
) : DialogWrapper(project) {
    private val settings = AiderProjectSettings.getInstance(project)
    private val settingsButton = createSettingsButton()
    private val customActionComboBox = ComboBox<CustomActionConfiguration>().apply {
        renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (component is JLabel && value is CustomActionConfiguration) {
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
        emptyText.text = "Enter additional instructions (optional)"
    }

    init {
        title = "Execute Custom Action"
        init()
        updateCustomActions()
    }

    private fun updateCustomActions() {
        customActionComboBox.removeAllItems()
        settings.getCustomActions()
            .filter { it.isEnabled }
            .forEach { customActionComboBox.addItem(it) }
    }

    private fun updatePromptTemplate() {
        val selectedAction = getSelectedCustomAction()
        if (selectedAction != null) {
            val ellipsedTemplate = selectedAction.promptTemplate.let { template ->
                if (template.length > 100) {
                    template.take(97) + "..."
                } else {
                    template
                }
            }
            promptArea.emptyText.text = ellipsedTemplate
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
            description = "Open custom action settings"
        }
        return ActionButton(
            settingsAction, presentation, "CustomActionSettingsButton", ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
        )
    }

    override fun createCenterPanel(): JComponent {
        val panel = panel {
            row {
                label("Select the custom action to execute:")
            }
            row {
                cell(customActionComboBox)
                    .resizableColumn()
                    .align(AlignX.FILL)
                cell(settingsButton)
                    .align(AlignX.RIGHT)
            }
            row {
                label("Enter any additional instructions or requirements:")
            }
            row {
                cell(JBScrollPane(promptArea))
                    .resizableColumn()
                    .align(AlignY.FILL)
                    .align(AlignX.FILL)
            }.resizableRow()
        }
        
        panel.preferredSize = Dimension(800, 600)
        panel.minimumSize = Dimension(400, 300)
        return panel
    }

    override fun doValidate(): ValidationInfo? {
        val customAction = getSelectedCustomAction()
        return when {
            customAction == null -> ValidationInfo("Please select a custom action")
            settings.getCustomActions().isEmpty() -> ValidationInfo("No custom actions configured. Please configure custom actions in Project Settings.")
            else -> null
        }
    }

    override fun doOKAction() {
        val customAction = getSelectedCustomAction() ?: return
        val allFiles = FileTraversal.traverseFilesOrDirectories(selectedFiles)
        val settings = AiderSettings.getInstance()

        try {
            // Add context files to the file list - convert relative paths to absolute
            val absoluteCustomAction = customAction.withAbsolutePaths(project.basePath ?: "")
            val contextFiles = absoluteCustomAction.contextFiles.map { FileData(it, false) }
        
            val commandData = CommandData(
                message = buildPrompt(customAction, allFiles),
                useYesFlag = true,
                files = allFiles + contextFiles,
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
                "Failed to execute custom action: ${e.message}",
                "Custom Action Error"
            )
        }
    }

    private fun buildPrompt(customAction: CustomActionConfiguration, files: List<FileData>): String {
        return project.service<CustomActionPromptService>().buildPrompt(customAction, files, getAdditionalPrompt())
    }

    private fun getSelectedCustomAction(): CustomActionConfiguration? = customActionComboBox.selectedItem as? CustomActionConfiguration
    private fun getAdditionalPrompt(): String = promptArea.text
}
