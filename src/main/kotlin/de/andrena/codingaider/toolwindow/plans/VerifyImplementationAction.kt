package de.andrena.codingaider.toolwindow.plans

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.actions.aider.AiderAction
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.services.plans.AiderPlan
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.CommandDataCollector
import java.awt.Dimension
import java.io.File
import javax.swing.JComponent
import javax.swing.JTextArea
import com.intellij.openapi.diagnostic.Logger

class VerifyImplementationAction(
    private val project: Project,
    private val plan: AiderPlan
) : AnAction(
    "Verify Implementation",
    "Use LLM to verify implementation status and update checklist",
    AllIcons.Actions.CheckOut
) {
    
    companion object {
        private val LOG = Logger.getInstance(VerifyImplementationAction::class.java)
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        // Show confirmation dialog with preview
        val dialog = VerificationPreviewDialog(project, plan)
        if (dialog.showAndGet()) {
            performVerification()
        }
    }

    private fun performVerification() {
        LOG.info("Starting implementation verification for plan: ${plan.id}")
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Verifying Implementation", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "Preparing verification prompt..."
                    indicator.fraction = 0.1
                    
                    val verificationPrompt = createVerificationPrompt(plan)
                    LOG.debug("Created verification prompt for plan: ${plan.id}")
                    
                    indicator.text = "Configuring LLM settings..."
                    indicator.fraction = 0.3
                    
                    val settings = service<AiderSettings>()
                    val verificationLlm = if (settings.llm.isBlank()) null else settings.llm
                    LOG.info("Using LLM for verification: ${verificationLlm ?: "default"}")
                    
                    indicator.text = "Collecting command data..."
                    indicator.fraction = 0.5
                    
                    val commandData = CommandDataCollector.collectFromParameters(
                        plan.allFiles,
                        verificationPrompt,
                        project,
                        llm = verificationLlm
                    )
                    
                    indicator.text = "Executing verification..."
                    indicator.fraction = 0.8
                    
                    ApplicationManager.getApplication().invokeLater {
                        try {
                            AiderAction.executeAiderActionWithCommandData(project, commandData)
                            LOG.info("Successfully started verification execution for plan: ${plan.id}")
                        } catch (e: Exception) {
                            LOG.error("Failed to execute verification command for plan: ${plan.id}", e)
                            Messages.showErrorDialog(
                                project,
                                "Failed to execute verification: ${e.message}",
                                "Verification Execution Error"
                            )
                        }
                    }
                    
                    indicator.fraction = 1.0
                    
                } catch (e: Exception) {
                    LOG.error("Failed to perform verification for plan: ${plan.id}", e)
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            "Failed to perform verification: ${e.message}\n\nPlease check the logs for more details.",
                            "Verification Error"
                        )
                    }
                }
            }
        })
    }

    private fun createVerificationPrompt(plan: AiderPlan): String {
        val openItems = plan.openChecklistItems()
        val planContent = plan.plan
        
        return """
            Please analyze the current implementation status and update the checklist accordingly.
            
            PLAN CONTENT:
            $planContent
            
            CURRENT OPEN CHECKLIST ITEMS:
            ${openItems.joinToString("\n") { "- [ ] ${it.description}" }}
            
            INSTRUCTIONS:
            1. Analyze the current state of the files in the context
            2. For each open checklist item, determine if it has been implemented
            3. Update the checklist file by marking completed items as [x] instead of [ ]
            4. Only mark items as complete if they are fully implemented and working
            5. If any items are partially complete, leave them unchecked but add progress notes
            6. If the plan requires additional steps that are not in the checklist, add them as new items
            7. If checklist items contradict or conflict with the plan remove them from the checklist
            8. Pay special attention between discrepancies between the provided code and the plan. If required add new items to the checklist to address these discrepancies.
            
            Ensure you focus on the files listed in the plan context:
            Focus on updating the checklist file: ${plan.checklistPlanFile?.filePath ?: "${plan.mainPlanFile?.filePath?.replace(".md", "_checklist.md")}"}
            
            Be thorough but conservative - only mark items as complete if you're confident they work correctly.
        """.trimIndent()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = !plan.isPlanComplete()
    }
}

private class VerificationPreviewDialog(
    private val project: Project,
    private val plan: AiderPlan
) : DialogWrapper(project) {
    
    private val previewArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        text = createPreviewText()
    }
    
    init {
        title = "Verify Implementation - Preview"
        init()
    }
    
    private fun createPreviewText(): String {
        val openItems = plan.openChecklistItems()
        val totalItems = plan.totalChecklistItems()
        val completedItems = totalItems - openItems.size
        
        return """
            Plan: ${plan.mainPlanFile?.filePath?.substringAfterLast('/') ?: "Unknown"}
            Progress: $completedItems/$totalItems items completed
            
            This action will:
            1. Analyze the current implementation state of all files in the plan context
            2. Check each open checklist item against the actual code
            3. Automatically mark completed items as [x] in the checklist
            4. Add verification comments for completed items
            
            Open items to be verified:
            ${openItems.joinToString("\n") { "• ${it.description}" }}
            
            Files that will be analyzed:
            ${plan.allFiles.joinToString("\n") { "• ${it.filePath.substringAfterLast('/')}" }}
            
            The LLM will be conservative and only mark items as complete if they are fully implemented and working correctly.
        """.trimIndent()
    }
    
    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                scrollCell(JBScrollPane(previewArea))
                    .align(Align.FILL)
                    .resizableColumn()
            }.resizableRow()
        }.apply {
            preferredSize = Dimension(600, 400)
        }
    }
}
