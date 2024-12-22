package de.andrena.codingaider.inputdialog

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.settings.AiderProjectSettings
import javax.swing.JComponent

class AiderOptionsManager(
    private val project: Project,
    private val sharedOptionsPanel: AiderOptionsPanel,
    private val onOptionsChanged: () -> Unit
) {
    private val projectSettings = AiderProjectSettings.getInstance(project)

    val llmComboBox get() = sharedOptionsPanel.llmComboBox
    val yesCheckBox get() = sharedOptionsPanel.yesCheckBox
    val additionalArgsField get() = sharedOptionsPanel.additionalArgsField

    val panel: JComponent = panel {
        collapsibleGroup("Additional Options") {
            row {
                cell(sharedOptionsPanel)
                    .align(com.intellij.ui.dsl.builder.Align.FILL)
            }
        }.expanded(!projectSettings.isOptionsCollapsed)
            .onIsModified { projectSettings.isOptionsCollapsed = !it }
    }
}
