package de.andrena.codingaider.toolwindow.plans

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
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

    private val planViewer = MarkdownJcefViewer(listOf(AiderPlanService.AIDER_PLANS_FOLDER)).apply {
        setDarkTheme(!JBColor.isBright())
    }
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
        SwingUtilities.invokeLater {
            planViewer.setMarkdown(plan.plan)
        }
    }

    override fun createCenterPanel(): JComponent {
        // Create scroll panes with minimum sizes
        val markdownViewer = MarkdownJcefViewer(listOf(AiderPlanService.AIDER_PLANS_FOLDER)).apply {
            setDarkTheme(!JBColor.isBright())
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
        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, inputScrollPane).apply {
            dividerLocation = 400
            resizeWeight = 0.7 // Give more space to preview by default
            border = BorderFactory.createEmptyBorder()
        }
        return panel {
            row {
                cell(scrollPane)
                    .align(AlignX.FILL)
                    .align(AlignY.FILL)
                    .resizableColumn()
            }.resizableRow()
            row {
//                label("Plan:")
                cell(inputScrollPane)
                    .align(AlignX.FILL)
            }
        }.apply {
            border = BorderFactory.createEmptyBorder(8, 8, 8, 8)

            // Calculate optimal size based on screen dimensions
            val screenSize = java.awt.Toolkit.getDefaultToolkit().screenSize
            val optimalWidth = (screenSize.width * 0.7).toInt().coerceIn(600, 1200)
            val optimalHeight = (screenSize.height * 0.8).toInt().coerceIn(400, 800)

            preferredSize = Dimension(optimalWidth, optimalHeight)
            minimumSize = Dimension(500, 400)
        }

        // Create wrapper panel for proper sizing
//        return JPanel(BorderLayout()).apply {
//            add(splitPane, BorderLayout.CENTER)
//            minimumSize = Dimension(400, 300)
//            preferredSize = Dimension(800, 600)
//        }
    }

    fun getMessage(): String = refinementInput.text.trim()

    override fun getPreferredFocusedComponent(): JComponent = refinementInput
}
