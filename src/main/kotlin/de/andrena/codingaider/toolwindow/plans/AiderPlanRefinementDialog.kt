package de.andrena.codingaider.toolwindow.plans

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.outputview.MarkdownJcefViewer
import de.andrena.codingaider.services.plans.AiderPlan
import de.andrena.codingaider.services.plans.AiderPlanService
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

class AiderPlanRefinementDialog(
    project: Project,
    private val plan: AiderPlan
) : DialogWrapper(project) {
    private val messageArea = JTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
        rows = 5
        font = UIManager.getFont("TextField.font")
        border = UIManager.getBorder("TextField.border")
    }
    private val markdownViewer = MarkdownJcefViewer(listOf(AiderPlanService.AIDER_PLANS_FOLDER))
    private var isMarkdownLoaded = false

    init {
        title = "Refine Plan"
        init()
    }

    override fun show() {
        super.show()
        SwingUtilities.invokeLater {
            if (!isMarkdownLoaded) {
                markdownViewer.setDarkTheme(!JBColor.isBright())
                markdownViewer.setMarkdown(plan.plan)
                isMarkdownLoaded = true
                markdownViewer.component.revalidate()
                markdownViewer.component.repaint()
            }
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        
        // Configure markdown viewer panel with its own constraints
        val previewScrollPane = JBScrollPane(markdownViewer.component).apply {
            minimumSize = Dimension(400, 200)
        }
        val constraints1 = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            weightx = 1.0
            weighty = 0.7
            fill = GridBagConstraints.BOTH
            anchor = GridBagConstraints.NORTH
        }
        panel.add(createGroupPanel("Current Plan", previewScrollPane), constraints1)
        
        // Configure message area panel with separate constraints
        val messageScrollPane = JBScrollPane(messageArea).apply {
            minimumSize = Dimension(400, 100)
        }
        val constraints2 = GridBagConstraints().apply {
            gridx = 0
            gridy = 1
            weightx = 1.0
            weighty = 0.3
            fill = GridBagConstraints.BOTH
            anchor = GridBagConstraints.NORTH
        }
        panel.add(createGroupPanel("Refinement Request:", messageScrollPane, createCommentLabel()), constraints2)
        
        return panel
    }

    private fun createGroupPanel(title: String, component: JComponent, extraComponent: JComponent? = null): JPanel {
        return panel {
            group(title) {
                row {
                    cell(component)
                        .align(Align.FILL)
                        .resizableColumn()
                }.resizableRow()
                
                extraComponent?.let {
                    row {
                        cell(it)
                            .align(Align.FILL)
                    }
                }
            }
        }
    }

    private fun createCommentLabel(): JLabel {
        return JLabel("""
            <html>Describe how you want to refine or extend the plan.<br>
            This may create subplans if the changes are substantial or if you prompt the LLM to do so.</html>
        """.trimIndent()).apply {
            border = BorderFactory.createEmptyBorder(5, 0, 5, 0)
            foreground = UIManager.getColor("Label.infoForeground")
        }
    }

    override fun getPreferredFocusedComponent(): JComponent = messageArea

    fun getMessage(): String {
        return messageArea.text.trim()
    }
}
