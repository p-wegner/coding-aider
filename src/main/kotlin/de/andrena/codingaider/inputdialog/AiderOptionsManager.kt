package de.andrena.codingaider.inputdialog

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.Project
import com.intellij.ui.components.panels.Wrapper
import de.andrena.codingaider.settings.AiderProjectSettings
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.PanelAnimation
import java.awt.Dimension
import javax.swing.JComponent

class AiderOptionsManager(
    private val project: Project,
    private val apiKeyChecker: ApiKeyChecker,
    private val onOptionsChanged: () -> Unit
) {
    private val projectSettings = AiderProjectSettings.getInstance(project)
    private val optionsPanel = AiderOptionsPanel(project, apiKeyChecker)
    private val flagAndArgsPanel by lazy { optionsPanel }
    private val wrappedOptionsPanel by lazy {
        Wrapper().apply {
            setContent(flagAndArgsPanel)
            isVisible = true
        }
    }
    private val panelAnimation = PanelAnimation(wrappedOptionsPanel)

    val panel: JComponent get() = wrappedOptionsPanel
    val llmComboBox get() = optionsPanel.llmComboBox
    val yesCheckBox get() = optionsPanel.yesCheckBox
    val additionalArgsField get() = optionsPanel.additionalArgsField

    init {
        updatePanelSize(projectSettings.isOptionsCollapsed)
    }

    fun createCollapseButton(): JPanel {
        val action = object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                val isCollapsed = projectSettings.isOptionsCollapsed
                projectSettings.isOptionsCollapsed = !isCollapsed
                
                val startHeight = wrappedOptionsPanel.height
                val endHeight = if (isCollapsed) flagAndArgsPanel.preferredSize.height else 0
                
                panelAnimation.animate(startHeight, endHeight) {
                    updateCollapseButtonIcon(!isCollapsed)
                    updatePanelSize(!isCollapsed)
                }
            }
        }
        
        val presentation = Presentation("Additional Options").apply {
            icon = if (projectSettings.isOptionsCollapsed) AllIcons.General.ArrowRight else AllIcons.General.ArrowDown
            description = if (projectSettings.isOptionsCollapsed) "Show additional options" else "Hide additional options"
        }
        
        val button = ActionButton(action, presentation, "AiderOptionsButton", Dimension(Int.MAX_VALUE, 28))
        
        return JPanel(BorderLayout()).apply {
            add(button, BorderLayout.CENTER)
            background = UIManager.getColor("Tree.background")
            border = JBUI.Borders.empty(2)
        }
    }

    private fun updatePanelSize(collapsed: Boolean) {
        wrappedOptionsPanel.apply {
            if (collapsed) {
                minimumSize = Dimension(0, 0)
                maximumSize = Dimension(Int.MAX_VALUE, 0)
                preferredSize = Dimension(0, 0)
            } else {
                minimumSize = null
                maximumSize = null
                preferredSize = null
            }
            revalidate()
            repaint()
            onOptionsChanged()
        }
    }

    private fun updateCollapseButtonIcon(collapsed: Boolean) {
        collapseButton.presentation.apply {
            icon = if (collapsed) AllIcons.General.ArrowRight else AllIcons.General.ArrowDown
            description = if (collapsed) "Show Options" else "Hide Options"
        }
    }

    val collapseButton: JPanel = createCollapseButton()
}
