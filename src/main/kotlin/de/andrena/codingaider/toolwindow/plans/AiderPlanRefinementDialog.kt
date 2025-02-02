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
        // Create a Markdown viewer to show the current plan content.
        val planViewer = MarkdownJcefViewer(listOf(AiderPlanService.AIDER_PLANS_FOLDER)).apply {
            setDarkTheme(!JBColor.isBright())
            setMarkdown(plan.plan)
        }
        val previewScrollPane = JScrollPane(planViewer.component).apply {
            preferredSize = Dimension(600, 300)
        }
        val inputScrollPane = JScrollPane(refinementInput).apply {
            preferredSize = Dimension(600, 150)
        }
        // Create a split pane with preview at top and input below.
        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, previewScrollPane, inputScrollPane)
        splitPane.dividerLocation = 300
        splitPane.resizeWeight = 0.5
        return splitPane
    }

    fun getMessage(): String = refinementInput.text.trim()

    override fun getPreferredFocusedComponent(): JComponent = refinementInput
}
