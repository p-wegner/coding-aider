package de.andrena.codingaider.toolwindow.plans

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.outputview.CleanMarkdownJcefViewer
import de.andrena.codingaider.services.plans.AiderPlan
import de.andrena.codingaider.services.plans.AiderPlanService
import java.awt.Dimension
import java.awt.event.KeyEvent
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

    override fun init() {
        super.init()
    }

    override fun createActions(): Array<Action> {
        val actions = super.createActions()
        (actions[0] as? DialogWrapperAction)?.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_O)
        (actions[1] as? DialogWrapperAction)?.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C)
        return actions
    }

    override fun createCenterPanel(): JComponent {
        // Create markdown viewer and scroll pane
        val markdownViewer = CleanMarkdownJcefViewer(listOf(AiderPlanService.AIDER_PLANS_FOLDER)).apply {
            setDarkTheme(!JBColor.isBright())
            setMarkdown(plan.plan)
        }

        val scrollPane = JBScrollPane(markdownViewer.component).apply {
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            border = BorderFactory.createEmptyBorder()

            // Set minimum size for better initial layout
            minimumSize = Dimension(400, 300)
            preferredSize = Dimension(800, 600)
        }
        val inputScrollPane = JBScrollPane(refinementInput).apply {
            minimumSize = Dimension(400, 100)
            preferredSize = Dimension(800, 200)
        }
        // Create a responsive split pane
        return panel {
            row { label("Plan: ${plan.id}") }
//            row {
//                cell(scrollPane)
//                    .align(AlignX.FILL)
//                    .align(AlignY.FILL)
//                    .resizableColumn()
//            }.resizableRow()
            row {
//                label("Plan:")
                cell(inputScrollPane)
                    .align(AlignX.FILL)
                    .align(AlignY.FILL)
                    .resizableColumn()
            }.resizableRow()
        }.apply {
            border = BorderFactory.createEmptyBorder(8, 8, 8, 8)

            // Calculate optimal size based on screen dimensions
            val screenSize = java.awt.Toolkit.getDefaultToolkit().screenSize
            val optimalWidth = (screenSize.width * 0.7).toInt().coerceIn(600, 1200)
            val optimalHeight = (screenSize.height * 0.8).toInt().coerceIn(400, 800)

            preferredSize = Dimension(optimalWidth, optimalHeight)
            minimumSize = Dimension(500, 400)
        }

    }

    fun getMessage(): String = refinementInput.text.trim()

    override fun getPreferredFocusedComponent(): JComponent = refinementInput
}
