package de.andrena.codingaider.outputview

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.outputview.markdown.MarkdownViewer
import de.andrena.codingaider.services.RunningCommandService
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.services.plans.ContinuePlanService
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import java.awt.BorderLayout
import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.*

class AiderOutputTab(
    private val project: Project,
    private val initialTitle: String,
    initialText: String,
    private val onAbort: Abortable?,
    private val displayString: String?,
    private val commandData: CommandData? = null
): CodingAiderOutputPresentation {
    private val markdownViewer = MarkdownViewer(listOf(AiderPlanService.AIDER_PLANS_FOLDER))
    private val mainPanel = JPanel(BorderLayout())
    private val isProcessFinished = AtomicBoolean(false)
    private val isDisposed = AtomicBoolean(false)
    
    private var autoCloseTimer: java.util.concurrent.ScheduledFuture<*>? = null
    private val executor = com.intellij.util.concurrency.AppExecutorUtil.getAppScheduledExecutorService()
    
    // Buttons for tab toolbar
    private val abortButton = JButton("Abort").apply {
        mnemonic = KeyEvent.VK_A
        icon = AllIcons.Actions.Suspend
        isVisible = onAbort != null
        addActionListener {
            if (!isProcessFinished.get()) {
                onAbort?.abortCommand(commandData?.planId)
                setProcessFinished()
            }
        }
    }
    
    private val closeAndContinueButton = JButton("Continue").apply {
        mnemonic = KeyEvent.VK_N
        icon = AllIcons.Actions.Execute
        isVisible = false
        addActionListener {
            if (isProcessFinished.get()) {
                try {
                    isEnabled = false
                    text = "Continuing..."
                    project.service<ContinuePlanService>().continuePlan()
                } catch (e: Exception) {
                    isEnabled = true
                    text = "Continue"
                    JOptionPane.showMessageDialog(
                        mainPanel,
                        "Error during plan continuation: ${e.message}",
                        "Continuation Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
    }
    
    private val createPlanButton = JButton("Create Plan").apply {
        mnemonic = KeyEvent.VK_P
        isVisible = false
        toolTipText = "Convert this command and output into a structured plan"
        icon = AllIcons.Actions.RunAll
        addActionListener { onCreatePlanClicked() }
    }
    
    private val showDevToolsButton = JButton("DevTools").apply {
        mnemonic = KeyEvent.VK_D
        isVisible = false
        toolTipText = "Open Chrome DevTools for debugging the markdown viewer"
        icon = AllIcons.Actions.StartDebugger
        addActionListener { 
            isEnabled = false
            if (markdownViewer.showDevTools()) {
                text = "DevTools Opened"
            } else {
                text = "DevTools Failed"
                executor.schedule({
                    SwingUtilities.invokeLater {
                        text = "DevTools"
                        isEnabled = true
                    }
                }, 2000, java.util.concurrent.TimeUnit.MILLISECONDS)
            }
        }
    }

    val component: JComponent
        get() = mainPanel
        
    val title: String
        get() = displayString ?: initialTitle

    init {
        val scrollPane = JBScrollPane(markdownViewer.component).apply {
            border = null
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        }
        
        val contentPanel = JPanel(BorderLayout(0, 0)).apply {
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            add(scrollPane, BorderLayout.CENTER)
        }
        
        mainPanel.add(contentPanel, BorderLayout.CENTER)
        
        // Create toolbar with buttons
        val toolbar = createToolbar()
        if (toolbar.componentCount > 0) {
            mainPanel.add(toolbar, BorderLayout.SOUTH)
        }
        
        markdownViewer.setMarkdown(initialText)
        
        showDevToolsButton.isVisible = markdownViewer.supportsDevTools() &&
                                       getInstance().showMarkdownDevTools
    }
    
    private fun createToolbar(): JPanel {
        val toolbar = JPanel()
        
        if (onAbort != null) {
            toolbar.add(abortButton)
        }
        toolbar.add(closeAndContinueButton)
        toolbar.add(createPlanButton)
        toolbar.add(showDevToolsButton)
        
        return toolbar
    }
    
    private fun onCreatePlanClicked() {
        if (isProcessFinished.get() && commandData != null) {
            try {
                createPlanButton.isEnabled = false
                createPlanButton.text = "Creating Plan..."

                project.service<RunningCommandService>().createPlanFromLastCommand(project)
            } catch (e: Exception) {
                createPlanButton.isEnabled = true
                createPlanButton.text = "Create Plan"
                JOptionPane.showMessageDialog(
                    mainPanel,
                    "Error during plan creation: ${e.message}",
                    "Plan Creation Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }

    fun updateProgress(output: String, title: String) {
        if (isDisposed.get()) return
        
        SwingUtilities.invokeLater {
            try {
                val newContent = output.replace("\r\n", "\n")
                val contentToSet = newContent.ifBlank { " " }
                markdownViewer.setMarkdown(contentToSet)
            } catch (e: Exception) {
                println("Error updating tab content: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun setProcessFinished() {
        isProcessFinished.set(true)
        SwingUtilities.invokeLater {
            abortButton.isVisible = false
            closeAndContinueButton.isVisible = commandData?.structuredMode == true && 
                commandData.planId?.let { planId ->
                    project.service<de.andrena.codingaider.services.plans.ActivePlanService>()
                        .getActivePlan()?.let { !it.isPlanComplete() } ?: false
                } ?: false
            createPlanButton.isVisible = commandData != null && commandData.structuredMode != true
        }
    }
    
    fun startAutoCloseTimer(autocloseDelay: Int) {
        val settings = getInstance()
        if (!settings.enableAutoPlanContinue) return
        setProcessFinished()
        
        // For tool window tabs, we don't auto-close but we can still trigger auto continue
        if (settings.enableAutoPlanContinue && commandData?.structuredMode == true) {
            // Use a timer to trigger auto continue after the specified delay
            executor.schedule({
                try {
                    project.service<de.andrena.codingaider.services.AiderOutputService>().triggerAutoContinue(commandData)
                } catch (e: Exception) {
                    println("Error during auto continue: ${e.message}")
                    e.printStackTrace()
                }
            }, autocloseDelay.toLong(), java.util.concurrent.TimeUnit.SECONDS)
        }
    }

    fun dispose() {
        if (isDisposed.getAndSet(true)) return
        
        try {
            autoCloseTimer?.cancel(false)
            markdownViewer.dispose()
            mainPanel.removeAll()
        } catch (e: Exception) {
            println("Error disposing AiderOutputTab: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun hideElement() {
    }
}
