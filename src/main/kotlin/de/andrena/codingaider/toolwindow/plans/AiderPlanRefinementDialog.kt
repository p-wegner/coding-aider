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
import java.awt.BorderLayout
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
        minimumSize = Dimension(400, 100)
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
        // Create a Markdown viewer to show the current plan content.
        val planViewer = MarkdownJcefViewer(listOf(AiderPlanService.AIDER_PLANS_FOLDER)).apply {
            setDarkTheme(!JBColor.isBright())
            setMarkdown(plan.plan)
        }

        // Create scroll panes with minimum sizes
        val previewScrollPane = JBScrollPane(planViewer.component).apply {
            minimumSize = Dimension(400, 200)
            preferredSize = Dimension(800, 400)
        }
        val inputScrollPane = JBScrollPane(refinementInput).apply {
            minimumSize = Dimension(400, 100)
            preferredSize = Dimension(800, 200)
        }

        // Create a responsive split pane
        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, previewScrollPane, inputScrollPane).apply {
            dividerLocation = 400
            resizeWeight = 0.7 // Give more space to preview by default
            border = BorderFactory.createEmptyBorder()
        }

        // Create wrapper panel for proper sizing
        return JPanel(BorderLayout()).apply {
            add(splitPane, BorderLayout.CENTER)
            minimumSize = Dimension(400, 300)
            preferredSize = Dimension(800, 600)
        }
    }

    fun getMessage(): String = refinementInput.text.trim()

    override fun getPreferredFocusedComponent(): JComponent = refinementInput
}
