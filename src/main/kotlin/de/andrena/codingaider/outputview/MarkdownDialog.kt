package de.andrena.codingaider.outputview

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.inputdialog.AiderMode
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow


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

    private val markdownViewer = MarkdownJcefViewer(listOf(AiderPlanService.AIDER_PLANS_FOLDER)).apply {
        setMarkdown(initialText)
    }
    private val scrollPane = JBScrollPane(markdownViewer.component).apply {
        border = null
        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED

        // Track user scrolling to determine auto-scroll behavior
        verticalScrollBar.addAdjustmentListener { e ->
            if (!programmaticScrolling && e.valueIsAdjusting) {
                val scrollBar = verticalScrollBar
                // Check if user scrolled near the bottom (within 10 pixels)
                val isNearBottom = scrollBar.value >= (scrollBar.maximum - scrollBar.visibleAmount - 10)
                shouldAutoScroll = isNearBottom
            }
        }
    }

    // Flags to track scrolling behavior
    private var programmaticScrolling = false
    private var autoCloseTimer: TimerTask? = null
    private var refreshTimer: Timer? = null
    // Auto-scroll state - start with auto-scroll enabled
    private var shouldAutoScroll = true
    // Store panel expansion states between updates
    private val panelExpansionStates = mutableMapOf<String, Boolean>()
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
        foreground = JBColor(Color(0, 100, 0), Color(144, 238, 144)) // Dark green/light green
        addActionListener { onCreatePlanClicked() }
    }

    private fun onCreatePlanClicked() {
        if (isProcessFinished && commandData != null) {
            try {
                createPlanButton.isEnabled = false
                createPlanButton.text = "Creating Plan..."

                // Trigger plan creation using the already stored command data and output
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
                            // Ensure content is displayed after resize
                            markdownViewer.ensureContentDisplayed()
                        }
                    }
                }
            }
            
            // Also handle when the component is shown
            override fun componentShown(e: java.awt.event.ComponentEvent) {
                // Ensure content is displayed when dialog becomes visible
                invokeLater {
                    markdownViewer.ensureContentDisplayed()
                    
                    // Schedule another check after a short delay to catch any initialization issues
                    Timer().schedule(500) {
                        invokeLater {
                            markdownViewer.ensureContentDisplayed()
                        }
                    }
                }
            }
        })
        // Set optimal window dimensions based on screen size
        val screenSize = java.awt.Toolkit.getDefaultToolkit().screenSize
        val optimalWidth = (screenSize.width * 0.6).toInt().coerceIn(600, 1200)
        val optimalHeight = (screenSize.height * 0.7).toInt().coerceIn(400, 800)
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
        positionOnSameScreen()
    }

    private var lastContent = ""

    private var contentUpdateAttempts = 0
    private val maxContentUpdateAttempts = 5
    private var lastUpdateTime = System.currentTimeMillis()
    private var updateTimer: Timer? = null
    
    fun updateProgress(output: String, title: String) {
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            try {
                val newContent = output.replace("\r\n", "\n")
                if (newContent != lastContent || this@MarkdownDialog.title != title) {
                    lastContent = newContent
                    lastUpdateTime = System.currentTimeMillis()
                    contentUpdateAttempts = 0
                    
                    // Cancel any pending update timer
                    updateTimer?.cancel()
                    updateTimer = null

                    // Check if scrollbar is near the bottom before updating content
                    val scrollBar = scrollPane.verticalScrollBar
                    val wasNearBottom = scrollBar.value >= (scrollBar.maximum - scrollBar.visibleAmount - 10)
                    val scrollRatio = if (scrollBar.maximum > 0) 
                        scrollBar.value.toDouble() / scrollBar.maximum.toDouble() else 0.0

                    // Update content - ensure we're setting content even if it appears empty
                    val contentToSet = if (newContent.isBlank()) " " else newContent // Prevent empty content issues
                    markdownViewer.setMarkdown(contentToSet)
                    this@MarkdownDialog.title = title

                    // Schedule scroll adjustment after UI updates
                    invokeLater {
                        try {
                            programmaticScrolling = true // Prevent listener feedback loop
                            
                            // Wait a bit for panel restoration to affect layout
                            Timer().schedule(100) { // Increased delay for better rendering
                                invokeLater {
                                    // Force revalidate to ensure scrollbar values are updated
                                    scrollPane.revalidate()
                                    markdownViewer.component.revalidate()
                                    
                                    val newMax = scrollBar.maximum - scrollBar.visibleAmount
                                    
                                    // Scroll to bottom if auto-scroll is enabled OR if we were already near the bottom
                                    if (shouldAutoScroll || wasNearBottom) {
                                        scrollBar.value = scrollBar.maximum // Scroll to the very bottom
                                        shouldAutoScroll = true // Ensure auto-scroll stays enabled if we scrolled to bottom
                                    } else if (scrollBar.maximum > 0) {
                                        // Try to maintain relative scroll position
                                        val targetValue = (scrollRatio * scrollBar.maximum).toInt()
                                        scrollBar.value = targetValue.coerceIn(0, scrollBar.maximum)
                                    }
                                    
                                    // Verify content was updated by checking component size
                                    if (markdownViewer.component.height <= 10 && contentUpdateAttempts < maxContentUpdateAttempts) {
                                        // Content might not have been properly rendered, schedule a retry
                                        contentUpdateAttempts++
                                        updateTimer = Timer()
                                        updateTimer?.schedule(300 * contentUpdateAttempts) {
                                            invokeLater {
                                                println("Retrying content update attempt $contentUpdateAttempts")
                                                markdownViewer.setMarkdown(contentToSet)
                                                markdownViewer.ensureContentDisplayed()
                                                scrollPane.revalidate()
                                                markdownViewer.component.revalidate()
                                            }
                                        }
                                    }
                                }
                            }
                        } finally {
                            programmaticScrolling = false
                        }
                    }
                    
                    // Set up a verification timer to check if content is visible after a delay
                    Timer().schedule(1000) {
                        invokeLater {
                            // If it's been more than 1 second since our last update and content still looks empty
                            if (System.currentTimeMillis() - lastUpdateTime >= 1000 && 
                                markdownViewer.component.height <= 10 && 
                                contentUpdateAttempts < maxContentUpdateAttempts) {
                                
                                println("Content verification failed, forcing content refresh")
                                // Force a complete refresh of the viewer
                                markdownViewer.ensureContentDisplayed()
                                scrollPane.revalidate()
                                markdownViewer.component.revalidate()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error updating markdown dialog: ${e.message}")
                e.printStackTrace()
                
                // Even if we encounter an error, try to ensure content is displayed
                try {
                    markdownViewer.ensureContentDisplayed()
                } catch (e2: Exception) {
                    println("Failed to recover from error: ${e2.message}")
                }
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
