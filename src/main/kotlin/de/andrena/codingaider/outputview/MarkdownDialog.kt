package de.andrena.codingaider.outputview

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.outputview.markdown.MarkdownViewer
import de.andrena.codingaider.services.RunningCommandService
import de.andrena.codingaider.services.plans.ActivePlanService
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.services.plans.ContinuePlanService
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import java.awt.BorderLayout
import java.awt.Color
import java.awt.EventQueue.invokeLater
import java.awt.Frame
import java.awt.event.KeyEvent
import java.awt.event.WindowEvent
import java.util.*
import java.util.Timer
import javax.swing.*
import kotlin.concurrent.schedule
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.math.max


class MarkdownDialog(
    private val project: Project,
    private val initialTitle: String,
    initialText: String,
    private val onAbort: Abortable?,
    private val displayString: String?,
    private val commandData: CommandData? = null
) : JDialog(null as Frame?, false), CodingAiderOutputPresentation {

    companion object {
        fun create(
            project: Project,
            initialTitle: String,
            initialText: String
        ): MarkdownDialog = MarkdownDialog(project, initialTitle, initialText, null, null)
    }

    override fun toString(): String {
        return displayString ?: initialTitle
    }

    private val markdownViewer = MarkdownViewer(listOf(AiderPlanService.AIDER_PLANS_FOLDER))
    private val scrollPane = JBScrollPane(markdownViewer.component).apply {
        border = null
        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED

    }

    private var autoCloseTimer: java.util.concurrent.ScheduledFuture<*>? = null
    private var refreshTimer: java.util.concurrent.ScheduledFuture<*>? = null
    private var keepOpenButton = JButton("Keep Open").apply {
        mnemonic = KeyEvent.VK_K
        isVisible = false
    }
    private var closeButton = JButton(onAbort?.let { "Abort" } ?: "Close").apply {
        mnemonic = onAbort?.let { KeyEvent.VK_A } ?: KeyEvent.VK_C
    }
    private var closeAndContinueButton = JButton("Close & Continue").apply {
        mnemonic = KeyEvent.VK_N
        isVisible = false
        addActionListener {
            if (isProcessFinished) {
                try {
                    isEnabled = false
                    text = "Continuing..."
                    dispose()
                    project.service<ContinuePlanService>().continuePlan()
                } catch (e: Exception) {
                    isEnabled = true
                    text = "Close & Continue"
                    JOptionPane.showMessageDialog(
                        this@MarkdownDialog,
                        "Error during plan continuation: ${e.message}",
                        "Continuation Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
    }

    private var createPlanButton = JButton("Create Plan").apply {
        mnemonic = KeyEvent.VK_P
        isVisible = false
        toolTipText =
            "Convert this command and output into a structured plan. " +
                    "This can help implement more complex features where single requests are not enough."
        icon = AllIcons.Actions.RunAll
        addActionListener { onCreatePlanClicked() }
    }
    
    private var showDevToolsButton = JButton("Show DevTools").apply {
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
                    invokeLater {
                        text = "Show DevTools"
                        isEnabled = true
                    }
                }, 2000, java.util.concurrent.TimeUnit.MILLISECONDS)
            }
        }
    }

    private fun onCreatePlanClicked() {
        if (isProcessFinished && commandData != null) {
            try {
                createPlanButton.isEnabled = false
                createPlanButton.text = "Creating Plan..."

                project.service<RunningCommandService>().createPlanFromLastCommand(project)

                dispose()
            } catch (e: Exception) {
                createPlanButton.isEnabled = true
                createPlanButton.text = "Create Plan"
                JOptionPane.showMessageDialog(
                    this@MarkdownDialog,
                    "Error during plan creation: ${e.message}",
                    "Plan Creation Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }

    private var isProcessFinished = false
    private var resizeTimer: java.util.concurrent.ScheduledFuture<*>? = null
    private val executor = com.intellij.util.concurrency.AppExecutorUtil.getAppScheduledExecutorService()
    private val logger = com.intellij.openapi.diagnostic.Logger.getInstance(MarkdownDialog::class.java)

    init {
        title = initialTitle

        // Add resize listener with debouncing
        addComponentListener(object : java.awt.event.ComponentAdapter() {
            override fun componentResized(e: java.awt.event.ComponentEvent) {
                resizeTimer?.cancel(false)
                resizeTimer = executor.schedule({
                    invokeLater {
                        markdownViewer.component.revalidate()
                        markdownViewer.component.repaint()
                        scrollPane.revalidate()
                        scrollPane.repaint()
                    }
                }, 150, java.util.concurrent.TimeUnit.MILLISECONDS)
            }
        })
        val screenSize = java.awt.Toolkit.getDefaultToolkit().screenSize
        val validScreenWidth = screenSize.width.coerceAtLeast(800)
        val validScreenHeight = screenSize.height.coerceAtLeast(600)
        
        val optimalWidth = (validScreenWidth * 0.6).toInt().coerceIn(600, 1200)
        val optimalHeight = (validScreenHeight * 0.7).toInt().coerceIn(400, 800)
        
        preferredSize = java.awt.Dimension(optimalWidth, optimalHeight)
        minimumSize = java.awt.Dimension(500, 400)

        layout = BorderLayout(10, 10)
        pack()
        setLocationRelativeTo(null)
        val contentPanel = JPanel(BorderLayout(0, 0)).apply {
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            add(scrollPane, BorderLayout.CENTER)
        }
        add(contentPanel, BorderLayout.CENTER)

        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosed(e: WindowEvent?) {
                try {
                    refreshTimer?.cancel(false)
                    refreshTimer = null
                    autoCloseTimer?.cancel(false)
                    autoCloseTimer = null
                    resizeTimer?.cancel(false)
                    resizeTimer = null
                    project.service<RunningCommandService>().removeRunningCommand(this@MarkdownDialog)
                } catch (ex: Exception) {
                    logger.error("Error during dialog cleanup", ex)
                } finally {
                    super.windowClosed(e)
                }
            }

            override fun windowClosing(windowEvent: java.awt.event.WindowEvent?) {
                if (isProcessFinished || onAbort == null) {
                    dispose()
                } else {
                    isProcessFinished = true  // Prevent multiple abort calls
                    onAbort.abortCommand(commandData?.planId)
                }
            }
        })

        val buttonPanel = JPanel()
        closeButton.apply {
            mnemonic = onAbort?.let { KeyEvent.VK_A } ?: KeyEvent.VK_C
            addActionListener {
                if (isProcessFinished || onAbort == null) {
                    dispose()
                } else {
                    onAbort.abortCommand(commandData?.planId)
                }
            }
        }
        keepOpenButton = JButton("Keep Open").apply {
            mnemonic = KeyEvent.VK_K
            addActionListener { cancelAutoClose() }
            isVisible = false
        }
        buttonPanel.add(closeButton)
        buttonPanel.add(closeAndContinueButton)
        buttonPanel.add(createPlanButton)
        buttonPanel.add(showDevToolsButton)
        buttonPanel.add(keepOpenButton)
        add(buttonPanel, BorderLayout.SOUTH)

        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosing(windowEvent: java.awt.event.WindowEvent?) {
                refreshTimer?.cancel(true)
                if (isProcessFinished || onAbort == null) {
                    dispose()
                } else {
                    onAbort.abortCommand(commandData?.planId)
                }
            }
        })
        markdownViewer.setMarkdown(initialText)
        
        showDevToolsButton.isVisible = markdownViewer.supportsDevTools() &&
                                       getInstance().showMarkdownDevTools
        
        positionOnSameScreen()
    }

    private var lastContent = ""

    fun updateProgress(output: String, title: String) {
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            try {
                val newContent = output.replace("\r\n", "\n")
                
                // Always update title
                this@MarkdownDialog.title = title
                
                // Only update content if it has changed
                if (newContent != lastContent) {
                    lastContent = newContent
                    val contentToSet = newContent.ifBlank { " " }
                    markdownViewer.setMarkdown(contentToSet)
                    
                }
            } catch (e: Exception) {
                println("Error updating markdown dialog: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun startAutoCloseTimer(autocloseDelay: Int) {
        val settings = getInstance()
        if (!settings.enableMarkdownDialogAutoclose) return
        
        keepOpenButton.isVisible = true
        var remainingSeconds = max(1, autocloseDelay)
        
        // Use AtomicReference to hold the future so we can cancel it from within the task
        val timerRef = java.util.concurrent.atomic.AtomicReference<java.util.concurrent.ScheduledFuture<*>?>()
        // Using scheduleWithFixedDelay instead of scheduleAtFixedRate for better hibernation support
        val task = executor.scheduleWithFixedDelay({
            invokeLater {
                if (remainingSeconds > 0) {
                    title = "$initialTitle - Closing in $remainingSeconds seconds"
                    remainingSeconds--
                } else {
                    try {
                        if (isProcessFinished) {
                            try {
                                if (getInstance().enableAutoPlanContinue && commandData?.structuredMode == true) {
                                    // Check if there's an active plan and it's not finished
                                    val activePlan = project.service<ActivePlanService>().getActivePlan()
                                    if (activePlan != null && !activePlan.isPlanComplete()) {
                                        project.service<ContinuePlanService>().continuePlan()
                                    }
                                }
                            } catch (e: Exception) {
                                logger.error("Error during autoclose continuation", e)
                            }
                        }
                    } catch (e: Exception) {
                        logger.error("Error during autoclose continuation", e)
                    } finally {
                        dispose()
                        timerRef.get()?.cancel(false)
                    }
                }
            }
        }, 0, 1000, java.util.concurrent.TimeUnit.MILLISECONDS)
        
        timerRef.set(task)
        autoCloseTimer = task
    }

    private fun cancelAutoClose() {
        autoCloseTimer?.cancel(false)
        autoCloseTimer = null
        keepOpenButton.isVisible = false
        title = initialTitle
    }

    fun setProcessFinished() {
        isProcessFinished = true
        invokeLater {
            closeButton.text = "Close"
            closeButton.mnemonic = KeyEvent.VK_C
            closeAndContinueButton.isVisible = commandData?.structuredMode == true
            createPlanButton.isVisible = commandData != null && commandData.structuredMode != true
        }
    }

    fun focus(delay: Long = 100) {
        executor.schedule({
            SwingUtilities.invokeLater {
                toFront()
                requestFocus()
                markdownViewer.component.requestFocusInWindow()
                // Set dark theme based on current IDE theme
                markdownViewer.setMarkdown(lastContent)
            }
        }, delay, java.util.concurrent.TimeUnit.MILLISECONDS)
    }


    fun positionOnSameScreen() {
        try {
            // Get the IDE frame to position relative to it
            val ideFrame = WindowManager.getInstance().getIdeFrame(project)
            val ideComponent = ideFrame?.component
            
            if (ideComponent != null && ideComponent.isShowing) {
                // Get IDE window bounds
                val ideLocation = ideComponent.locationOnScreen
                val ideSize = ideComponent.size
                
                // Get screen device where IDE is located
                val screenDevices = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
                val ideScreenBounds = screenDevices
                    .flatMap { it.configurations.toList() }
                    .map { it.bounds }
                    .firstOrNull { it.contains(ideLocation.x + ideSize.width/2, ideLocation.y + ideSize.height/2) }
                    ?: java.awt.Rectangle(0, 0, 1920, 1080) // Fallback if screen not found
                
                // Calculate centered position on the same screen as IDE
                val dialogSize = this.size
                val x = (ideScreenBounds.x + (ideScreenBounds.width - dialogSize.width) / 2)
                    .coerceIn(ideScreenBounds.x, ideScreenBounds.x + ideScreenBounds.width - dialogSize.width)
                val y = (ideScreenBounds.y + (ideScreenBounds.height - dialogSize.height) / 2)
                    .coerceIn(ideScreenBounds.y, ideScreenBounds.y + ideScreenBounds.height - dialogSize.height)
                
                // Set location with validated coordinates
                this.setLocation(x, y)
            } else {
                // Fallback to center on primary screen
                this.setLocationRelativeTo(null)
            }
        } catch (e: Exception) {
            // Fallback in case of any error
            println("Error positioning dialog: ${e.message}")
            this.setLocationRelativeTo(null)
        }
    }

    override fun hideElement() {
        isVisible = true
    }

}
