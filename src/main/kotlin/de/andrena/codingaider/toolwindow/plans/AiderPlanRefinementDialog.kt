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
import javax.swing.JComponent
import javax.swing.JTextArea
import javax.swing.UIManager

class AiderPlanRefinementDialog(
    project: Project,
    plan: AiderPlan
) : DialogWrapper(project) {
    private val messageArea = JTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
        rows = 5
        font = UIManager.getFont("TextField.font")
        border = UIManager.getBorder("TextField.border")
    }
    private val markdownViewer = MarkdownJcefViewer(listOf(AiderPlanService.AIDER_PLANS_FOLDER)).apply {
        setDarkTheme(!JBColor.isBright())
        setMarkdown(plan.plan)
    }

    init {
        title = "Refine Plan"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val messageScrollPane = JBScrollPane(messageArea)
        val previewScrollPane = JBScrollPane(markdownViewer.component).apply {
            preferredSize = Dimension(600, 300)
        }

        return panel {
            group("Current Plan") {
                row {
                    cell(previewScrollPane)
                        .align(Align.FILL)
                        .resizableColumn()
                }.resizableRow()
            }

            group("Refinement Request:") {
                row {
                    cell(messageScrollPane)
                        .align(Align.FILL)
                        .resizableColumn()
                        .comment(
                            """
                                        Describe how you want to refine or extend the plan.
                                        This may create subplans if the changes are substantial or if you prompt the LLM to do so.
                                    """.trimIndent()
                        )
                }.resizableRow()
            }
        }
    }

    override fun getPreferredFocusedComponent(): JComponent = messageArea

    fun getMessage(): String {
        return messageArea.text.trim()
    }
}
