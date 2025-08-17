package de.andrena.codingaider.outputview

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.outputview.markdown.MarkdownViewer
import de.andrena.codingaider.services.RunningCommandService
import de.andrena.codingaider.services.plans.ActivePlanService
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.settings.AiderSettings
import java.awt.BorderLayout
import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
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
                // Preserve existing content and add abort message
                val abortMessage = "\n\n---\n\n**Command Aborted**\n\nThe command was manually aborted by the user."
                val currentContent = getCurrentMarkdownContent()
                val updatedContent = currentContent + abortMessage
                markdownViewer.setMarkdown(updatedContent)
                
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
                    project.service<ActivePlanService>().continuePlan()
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

    private val cancelContinueButton = JButton("Cancel Continue").apply {
        mnemonic = KeyEvent.VK_C
        isVisible = false
        toolTipText = "Cancel scheduled plan continuation"
        addActionListener {
            autoCloseTimer?.cancel(false)
            isVisible = false
            countdownLabel.isVisible = false
            text = "Cancel Continue"
        }
    }
    
    private val autoContinueCheckbox = JCheckBox("Auto Continue", true).apply {
        mnemonic = KeyEvent.VK_U
        isVisible = false
        toolTipText = "Automatically continue with plan when finished"
    }
    
    private val countdownLabel = JLabel().apply {
        isVisible = false
    }
    
    private val autoScrollCheckbox = JCheckBox("Auto Scroll", true).apply {
        mnemonic = KeyEvent.VK_S
        toolTipText = "Automatically scroll to bottom when content is updated"
    }
    
    // Track current content for abort preservation
    private var currentMarkdownContent = initialText

    val component: JComponent
        get() = mainPanel
        
    val title: String
        get() = displayString ?: initialTitle

    init {
        val contentPanel = JPanel(BorderLayout(0, 0)).apply {
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            add(markdownViewer.component, BorderLayout.CENTER)
        }
        
        mainPanel.add(contentPanel, BorderLayout.CENTER)
        
        // Create toolbar with buttons
        val toolbar = createToolbar()
        if (toolbar.componentCount > 0) {
            mainPanel.add(toolbar, BorderLayout.SOUTH)
        }
        
        markdownViewer.setMarkdown(initialText)
        
        val settings = AiderSettings.getInstance()
        showDevToolsButton.isVisible = markdownViewer.supportsDevTools() && settings.showDevTools
        
        
        // Show auto-continue checkbox during execution if this is a structured mode command
        val hasActivePlan = commandData?.structuredMode == true && 
            commandData.planId?.let { planId ->
                project.service<de.andrena.codingaider.services.plans.ActivePlanService>()
                    .getActivePlan()?.let { !it.isPlanComplete() } ?: false
            } ?: false
        
        if (hasActivePlan) {
            autoContinueCheckbox.isVisible = true
            autoContinueCheckbox.isSelected = settings.enableAutoPlanContinue
        }
    }
    
    private fun createToolbar(): JPanel {
        val toolbar = JPanel()
        
        if (onAbort != null) {
            toolbar.add(abortButton)
        }
        toolbar.add(closeAndContinueButton)
        toolbar.add(createPlanButton)
        toolbar.add(showDevToolsButton)
        toolbar.add(autoContinueCheckbox)
        toolbar.add(autoScrollCheckbox)
        toolbar.add(cancelContinueButton)
        toolbar.add(countdownLabel)
        
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
                currentMarkdownContent = contentToSet // Track current content
                markdownViewer.setMarkdown(contentToSet)
                
                // Auto-scroll to bottom if enabled
                if (autoScrollCheckbox.isSelected) {
                    markdownViewer.scrollToBottom()
                }
                
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
            val hasActivePlan = commandData?.structuredMode == true && 
                commandData.planId?.let { planId ->
                    project.service<de.andrena.codingaider.services.plans.ActivePlanService>()
                        .getActivePlan()?.let { !it.isPlanComplete() } ?: false
                } ?: false
            closeAndContinueButton.isVisible = hasActivePlan
            autoContinueCheckbox.isVisible = hasActivePlan
            createPlanButton.isVisible = commandData != null && commandData.structuredMode != true
        }
    }
    
    fun startAutoCloseTimer(autocloseDelay: Int) {
        setProcessFinished()
        
        // Auto-continuation is now handled by ActivePlanService after command completion
        // This method only handles the UI countdown for user feedback
        if (commandData?.structuredMode == true && autoContinueCheckbox.isSelected) {
            val remaining = AtomicInteger(autocloseDelay)
            cancelContinueButton.isVisible = true
            countdownLabel.isVisible = true
            
            // Show initial countdown in toolbar
            SwingUtilities.invokeLater {
                countdownLabel.text = "Auto-continue in ${remaining.get()}s..."
            }
            
            autoCloseTimer = executor.scheduleWithFixedDelay({
                // Check if auto-continue is still enabled
                if (!autoContinueCheckbox.isSelected) {
                    autoCloseTimer?.cancel(false)
                    SwingUtilities.invokeLater {
                        cancelContinueButton.isVisible = false
                        countdownLabel.isVisible = false
                    }
                    return@scheduleWithFixedDelay
                }
                
                val secs = remaining.decrementAndGet()
                if (secs > 0) {
                    SwingUtilities.invokeLater {
                        countdownLabel.text = "Auto-continue in ${secs}s..."
                    }
                } else {
                    autoCloseTimer?.cancel(false)
                    SwingUtilities.invokeLater {
                        countdownLabel.text = "Auto-continuing..."
                        cancelContinueButton.isVisible = false
                        countdownLabel.isVisible = false
                    }
                }
            }, 1, 1, java.util.concurrent.TimeUnit.SECONDS)
        }
    }

    private fun getCurrentMarkdownContent(): String {
        return currentMarkdownContent
    }

    fun dispose() {
        if (isDisposed.getAndSet(true)) return
        
        try {
            // Abort the command if it's still running
            if (!isProcessFinished.get() && onAbort != null) {
                onAbort.abortCommand(commandData?.planId)
            }
            
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
