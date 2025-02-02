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
import javax.swing.*

class AiderPlanRefinementDialog(
    private val project: Project,
    private val plan: AiderPlan
) : DialogWrapper(project) {

    private val refinementInput = JTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
        font = UIManager.getFont("TextField.font")
        border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
        columns = 40
        rows = 8
        text = """Enter your plan refinement request here.
            
Examples:
- Add new requirements or features
- Break down complex tasks into subplans
- Modify implementation details
- Extend the plan based on feedback"""
    }

    init {
        title = "Refine Plan"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val markdownViewer = MarkdownJcefViewer(listOf(AiderPlanService.AIDER_PLANS_FOLDER)).apply {
            setDarkTheme(!JBColor.isBright())
            setMarkdown(plan.plan)
        }

        val previewScrollPane = JBScrollPane(markdownViewer.component).apply {
            preferredSize = Dimension(600, 300)
        }

        val inputScrollPane = JBScrollPane(refinementInput).apply {
            preferredSize = Dimension(600, 150)
        }

        return panel {
            group("Current Plan") {
                row {
                    cell(previewScrollPane)
                        .align(Align.FILL)
                        .resizableColumn()
                }.resizableRow()
            }
            
            group("Refinement Request") {
                row {
                    label("Enter your plan refinement request:")
                }
                row {
                    cell(inputScrollPane)
                        .align(Align.FILL)
                        .resizableColumn()
                }.resizableRow()
            }
        }
    }

    fun getMessage(): String = refinementInput.text.trim()

    override fun getPreferredFocusedComponent(): JComponent = refinementInput
}
