package de.andrena.codingaider.outputview

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.services.RunningCommandService
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.services.plans.ContinuePlanService
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import java.awt.BorderLayout
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

    private val markdownViewer = MarkdownJcefViewer(listOf(AiderPlanService.AIDER_PLANS_FOLDER)).apply {
        setMarkdown(initialText)
    }
    private val scrollPane = JBScrollPane(markdownViewer.component).apply {
        border = null
        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED

        verticalScrollBar.addAdjustmentListener { e ->
            if (!e.valueIsAdjusting) {
                val scrollBar = verticalScrollBar
                val isAtBottom = scrollBar.value >= scrollBar.maximum - scrollBar.visibleAmount - 10
                // Only update auto-scroll if user has manually scrolled
                if (shouldAutoScroll != isAtBottom) {
                    shouldAutoScroll = isAtBottom
                }
                // Store last manual scroll position
                if (!shouldAutoScroll) {
                    lastManualScrollPosition = scrollBar.value
                }
            }
        }
    }
    private var autoCloseTimer: TimerTask? = null
    private var refreshTimer: Timer? = null
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
    private var isProcessFinished = false
    private var shouldAutoScroll = true
    private var lastManualScrollPosition = 0
    private var isScrollAnimationInProgress = false
    private var lastScrollPosition = 0
    private var scrollAnimationTimer: Timer? = null
    private var resizeTimer: Timer? = null
    
    init {
        title = initialTitle
        markdownViewer.setMarkdown(initialText)
        
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


        // Start refresh timer
        refreshTimer = Timer().apply {
            scheduleAtFixedRate(0, 1000) {
                invokeLater {
                    markdownViewer.component.revalidate()
                    markdownViewer.component.repaint()
                }
            }
        }
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

    }

    private var lastContent = ""
    private var currentContent = ""
    private var isUpdating = false

    fun updateProgress(output: String, message: String) {
        if (isUpdating) return
        isUpdating = true

        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            try {
                val newContent = output.replace("\r\n", "\n")
                if (newContent != lastContent) {
                    lastContent = newContent

                    // Store scroll state before update
                    val scrollBar = scrollPane.verticalScrollBar
                    val wasAtBottom = scrollBar.value >= scrollBar.maximum - scrollBar.visibleAmount - 10
                    val prevViewportHeight = scrollPane.viewport.viewRect.height
                    val prevScrollPosition = scrollBar.value
                    val prevContentHeight = scrollPane.viewport.viewSize.height
                    val prevScrollRatio = if (prevContentHeight - prevViewportHeight > 0) {
                        prevScrollPosition.toDouble() / (prevContentHeight - prevViewportHeight)
                    } else 0.0

                    // Update scroll behavior based on user action
                    if (!wasAtBottom && shouldAutoScroll) shouldAutoScroll = false
                    if (wasAtBottom && !shouldAutoScroll) shouldAutoScroll = true

                    // Update content
                    markdownViewer.setMarkdown(newContent)
                    title = message

                    // Handle scrolling after content update
                    SwingUtilities.invokeLater {
                        val newContentHeight = scrollPane.viewport.viewSize.height
                        val viewportHeight = scrollPane.viewport.viewRect.height
                        val newMaxScroll = (newContentHeight - viewportHeight).coerceAtLeast(0)

                        when {
                            shouldAutoScroll && wasAtBottom -> {
                                smoothScrollTo(scrollBar, scrollBar.maximum)
                            }
                            prevContentHeight > 0 && newContentHeight > 0 -> {
                                val targetPosition = (newMaxScroll * prevScrollRatio).toInt()
                                    .coerceIn(0, newMaxScroll)
                                if (Math.abs(targetPosition - scrollBar.value) > viewportHeight / 3) {
                                    smoothScrollTo(scrollBar, targetPosition)
                                } else {
                                    scrollBar.value = targetPosition
                                }
                            }
                        }

                        scrollPane.repaint()
                        isUpdating = false
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
        }
    }

    fun focus(delay: Long = 100) {
        Timer().schedule(delay) {
            SwingUtilities.invokeLater {
                toFront()
                requestFocus()
                markdownViewer.component.requestFocusInWindow()
                // Set dark theme based on current IDE theme
                currentContent = lastContent
                markdownViewer.setMarkdown(currentContent)
            }
        }
    }

    private fun smoothScrollTo(scrollBar: JScrollBar, targetValue: Int) {
        if (isScrollAnimationInProgress) {
            scrollAnimationTimer?.cancel()
        }

        isScrollAnimationInProgress = true
        val startValue = scrollBar.value
        val distance = targetValue - startValue
        val duration = 300L // Total animation duration in ms
        val startTime = System.currentTimeMillis()

        // Cancel any existing animation
        scrollAnimationTimer?.cancel()
        
        // Create new animation timer
        scrollAnimationTimer = Timer().apply {
            scheduleAtFixedRate(0, 16) { // ~60fps
                val currentTime = System.currentTimeMillis()
                val elapsed = (currentTime - startTime).toFloat()
                val progress = (elapsed / duration).coerceIn(0f, 1f)
                
                if (progress >= 1f) {
                    SwingUtilities.invokeLater {
                        scrollBar.value = targetValue
                        lastScrollPosition = targetValue
                        isScrollAnimationInProgress = false
                    }
                    cancel()
                    return@scheduleAtFixedRate
                }

                // Enhanced easing function for smoother motion
                val easedProgress = easeInOutCubic(progress)
                val currentValue = startValue + (distance * easedProgress).toInt()

                SwingUtilities.invokeLater {
                    scrollBar.value = currentValue
                    lastScrollPosition = currentValue
                }
            }
        }
    }

    private fun easeInOutCubic(x: Float): Float {
        return if (x < 0.5f) {
            4 * x * x * x
        } else {
            1 - (-2 * x + 2).pow(3) / 2
        }
    }
}
