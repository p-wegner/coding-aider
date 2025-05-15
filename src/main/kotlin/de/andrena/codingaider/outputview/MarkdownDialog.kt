package de.andrena.codingaider.outputview

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.services.RunningCommandService
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
) : JDialog(null as Frame?, false) {

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

    private val markdownViewer = MarkdownJcefViewer(listOf(AiderPlanService.AIDER_PLANS_FOLDER))
    private val scrollPane = JBScrollPane(markdownViewer.component).apply {
        border = null
        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED

        // Track user scrolling to determine auto-scroll behavior
        verticalScrollBar.addAdjustmentListener { e ->
            // Only consider manual scrolling (not programmatic)
            if (!programmaticScrolling) {
                val scrollBar = verticalScrollBar
                // Check if user scrolled near the bottom (within 20 pixels)
                val isNearBottom = scrollBar.value >= (scrollBar.maximum - scrollBar.visibleAmount - 20)
                
                // Only update auto-scroll state on actual user interaction
                if (e.valueIsAdjusting) {
                    shouldAutoScroll = isNearBottom
                }
            }
        }
    }

    // Flag to track programmatic scrolling to avoid feedback loops
    private var programmaticScrolling = false
    private var autoCloseTimer: TimerTask? = null
    private var refreshTimer: Timer? = null
    // Auto-scroll state - start with auto-scroll enabled
    private var shouldAutoScroll = true
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
        // TODO 02.05.2025 pwegner: think about if colors make sense or where to place the button
//        foreground = JBColor(Color(0, 100, 0), Color(144, 238, 144)) // Dark green/light green
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
                Timer().schedule(2000) {
                    invokeLater {
                        text = "Show DevTools"
                        isEnabled = true
                    }
                }
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
    private var resizeTimer: Timer? = null

    init {
        title = initialTitle

        // Add resize listener with debouncing
        addComponentListener(object : java.awt.event.ComponentAdapter() {
            override fun componentResized(e: java.awt.event.ComponentEvent) {
                resizeTimer?.cancel()
                resizeTimer = Timer().apply {
                    schedule(150) { // Debounce resize events
                        invokeLater {
                            markdownViewer.component.revalidate()
                            markdownViewer.component.repaint()
                            scrollPane.revalidate()
                            scrollPane.repaint()
                        }
                    }
                }
            }
        })
        // Set optimal window dimensions based on screen size with better bounds checking
        val screenSize = java.awt.Toolkit.getDefaultToolkit().screenSize
        // Ensure screen size is reasonable (handle multi-monitor setups better)
        val validScreenWidth = screenSize.width.coerceAtLeast(800)
        val validScreenHeight = screenSize.height.coerceAtLeast(600)
        
        // Calculate dimensions as percentage of screen but with reasonable min/max values
        val optimalWidth = (validScreenWidth * 0.6).toInt().coerceIn(600, 1200)
        val optimalHeight = (validScreenHeight * 0.7).toInt().coerceIn(400, 800)
        
        // Set size with validated dimensions
        preferredSize = java.awt.Dimension(optimalWidth, optimalHeight)
        minimumSize = java.awt.Dimension(500, 400)

        // Use weighted layout for better content scaling
        layout = BorderLayout(10, 10)
        pack()
        setLocationRelativeTo(null)
        // Add scroll pane with proper weighting
        val contentPanel = JPanel(BorderLayout(0, 0)).apply {
            border = javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5)
            add(scrollPane, BorderLayout.CENTER)
        }
        add(contentPanel, BorderLayout.CENTER)

        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosed(e: WindowEvent?) {
                try {
                    refreshTimer?.cancel()
                    refreshTimer = null
                    autoCloseTimer?.cancel()
                    autoCloseTimer = null
                    resizeTimer?.cancel()
                    resizeTimer = null
                    project.service<RunningCommandService>().removeRunningCommand(this@MarkdownDialog)
                } catch (ex: Exception) {
                    println("Error during dialog cleanup: ${ex.message}")
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
                refreshTimer?.cancel()
                if (isProcessFinished || onAbort == null) {
                    dispose()
                } else {
                    onAbort.abortCommand(commandData?.planId)
                }
            }
        })
        markdownViewer.setMarkdown(initialText)
        
        // Check if DevTools are supported and if the setting is enabled
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

                    // Check if scrollbar is near the bottom before updating content
                    val scrollBar = scrollPane.verticalScrollBar
                    val wasNearBottom = scrollBar.value >= (scrollBar.maximum - scrollBar.visibleAmount - 10)

                    // Update content - force a non-empty string
                    val contentToSet = newContent.ifBlank { " " }
                    markdownViewer.setMarkdown(contentToSet)

                    // Schedule multiple scroll attempts with increasing delays
                    // This helps ensure we reach the bottom even with dynamic content rendering
                    val scrollTimers = mutableListOf<TimerTask>()
                    
                    for (delay in listOf(100L, 300L, 600L, 1000L)) {
                        scrollTimers.add(Timer().schedule(delay) {
                            invokeLater {
                                try {
                                    // Scroll to bottom if auto-scroll is enabled OR if we were already near the bottom
                                    if (shouldAutoScroll || wasNearBottom) {
                                        programmaticScrolling = true // Prevent listener feedback loop
                                        scrollBar.value = scrollBar.maximum
                                        
                                        // Only the last timer should reset the flag
                                        if (delay == 1000L) {
                                            programmaticScrolling = false
                                        }
                                    }
                                } catch (e: Exception) {
                                    programmaticScrolling = false
                                    println("Error during scrolling at ${delay}ms: ${e.message}")
                                }
                            }
                        })
                    }
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
        autoCloseTimer = Timer().scheduleAtFixedRate(0, 1000) { // Update every second
            invokeLater {
                if (remainingSeconds > 0) {
                    title = "$initialTitle - Closing in $remainingSeconds seconds"
                    remainingSeconds--
                } else {
                    try {
                        if (isProcessFinished) {
                            try {
                                if (getInstance().enableAutoPlanContinue && commandData?.structuredMode == true) {
                                    project.service<ContinuePlanService>().continuePlan()
                                }
                            } catch (e: Exception) {
                                println("Error during autoclose continuation: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        println("Error during autoclose continuation: ${e.message}")
                    } finally {
                        dispose()
                        autoCloseTimer?.cancel()
                    }
                }
            }
        }
    }

    private fun cancelAutoClose() {
        autoCloseTimer?.cancel()
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
        Timer().schedule(delay) {
            SwingUtilities.invokeLater {
                toFront()
                requestFocus()
                markdownViewer.component.requestFocusInWindow()
                // Set dark theme based on current IDE theme
                markdownViewer.setMarkdown(lastContent)
            }
        }
    }


    fun positionOnSameScreen() {
        // Position dialog relative to IDE window
        val ideFrame = WindowManager.getInstance().getIdeFrame(project)
        ideFrame?.component?.let { parent ->
            setLocationRelativeTo(parent)
        }
    }

}
