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

    private val markdownViewer = MarkdownJcefViewer().apply {
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

    init {
        title = initialTitle
        markdownViewer.setMarkdown(initialText)


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
        scrollPane.border = javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5)
        add(scrollPane, BorderLayout.CENTER)
        scrollPane.preferredSize = java.awt.Dimension(
            (preferredSize.width * 0.95).toInt(),
            (preferredSize.height * 0.9).toInt()
        )

        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosed(e: WindowEvent?) {
                try {
                    refreshTimer?.cancel()
                    refreshTimer = null
                    autoCloseTimer?.cancel()
                    autoCloseTimer = null
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

                    // Get scroll position before update
                    val scrollBar = scrollPane.verticalScrollBar
                    val wasAtBottom = scrollBar.value >= scrollBar.maximum - scrollBar.visibleAmount - 10

                    // If user scrolled up, disable auto-scroll
                    if (!wasAtBottom && shouldAutoScroll) {
                        shouldAutoScroll = false
                    }
                    // If user scrolled to bottom, enable auto-scroll
                    if (wasAtBottom && !shouldAutoScroll) {
                        shouldAutoScroll = true
                    }

                    // Capture precise scroll state before update
                    val prevViewportHeight = scrollPane.viewport.viewRect.height
                    val prevScrollPosition = scrollBar.value
                    val prevContentHeight = scrollPane.viewport.viewSize.height

                    // Update content
                    markdownViewer.setMarkdown(newContent)
                    title = message

                    // Handle scrolling after content update
                    SwingUtilities.invokeLater {
                        // Calculate after content has rendered
                        val newContentHeight = scrollPane.viewport.viewSize.height
                        val viewportHeight = scrollPane.viewport.viewRect.height
                        
                        if (shouldAutoScroll && wasAtBottom) {
                            // Direct scroll to bottom without animation if we were at bottom
                            scrollBar.value = scrollBar.maximum
                        } else if (prevContentHeight > 0 && newContentHeight > 0) {
                            // Calculate relative position based on visible content
                            val relativePosition = (prevScrollPosition + viewportHeight/2).toDouble() / prevContentHeight
                            val newPosition = (newContentHeight * relativePosition - viewportHeight/2).toInt()
                            
                            // Apply bounded scroll position
                            scrollBar.value = newPosition.coerceIn(
                                scrollBar.minimum, 
                                (scrollBar.maximum - scrollBar.visibleAmount).coerceAtLeast(scrollBar.minimum)
                            )
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
                val isDark = !JBColor.isBright()
                currentContent = lastContent
                markdownViewer.setMarkdown(currentContent)
            }
        }
    }

    private fun smoothScrollTo(scrollBar: JScrollBar, targetValue: Int) {
        if (isScrollAnimationInProgress) return

        isScrollAnimationInProgress = true
        val startValue = scrollBar.value
        val distance = targetValue - startValue
        val steps = 15 // Increased for smoother animation
        val delay = 12L // Slightly faster (~83fps)

        Thread {
            try {
                for (i in 1..steps) {
                    val progress = i.toFloat() / steps
                    // Enhanced easing function for smoother acceleration/deceleration
                    val easedProgress = (1 - Math.cos(progress * Math.PI)) / 2
                    val currentValue = startValue + (distance * easedProgress).toInt()

                    SwingUtilities.invokeLater {
                        scrollBar.value = currentValue
                    }

                    Thread.sleep(delay)
                }
                // Ensure we reach exact target
                SwingUtilities.invokeLater {
                    scrollBar.value = targetValue
                }
            } finally {
                isScrollAnimationInProgress = false
            }
        }.start()
    }
}
