package de.andrena.codingaider.outputview

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import de.andrena.codingaider.services.RunningCommandService
import de.andrena.codingaider.services.plans.AiderPlanService
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
    private val displayString: String?
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

    private val markdownViewer = CustomMarkdownViewer(listOf(AiderPlanService.AIDER_PLANS_FOLDER)).apply {
        setDarkTheme(!JBColor.isBright())
    }
    private val scrollPane = JBScrollPane(markdownViewer.component).apply {
        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        viewport.scrollMode = JViewport.BACKINGSTORE_SCROLL_MODE
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
                val runningService = project.service<RunningCommandService>()
                val planService = project.service<AiderPlanService>()
                val plans = planService.getAiderPlans()
                val currentPlan = plans.firstOrNull { plan -> !plan.isPlanComplete() }
                
                if (currentPlan != null) {
                    project.service<ContinuePlanService>().continuePlan(currentPlan)
                }
                dispose()
            }
        }
    }
    private var isProcessFinished = false
    private var autoScroll = true

    init {
        title = initialTitle
        markdownViewer.setMarkdownContent(initialText)

        // Add scroll listener to detect when user manually scrolls
        scrollPane.verticalScrollBar.addAdjustmentListener { e ->
            val scrollBar = scrollPane.verticalScrollBar
            val extent = scrollBar.model.extent
            val maximum = scrollBar.model.maximum
            val current = scrollBar.model.value
            
            // Check if we're within 20 pixels of the bottom
            val isAtBottom = (current + extent + 20) >= maximum
            
            // Update autoScroll when:
            // 1. User is manually scrolling (valueIsAdjusting is true)
            // 2. Or when they've scrolled to the bottom
            if (scrollBar.valueIsAdjusting || isAtBottom) {
                autoScroll = isAtBottom
            }
        }

        // Start refresh timer
        refreshTimer = Timer().apply {
            scheduleAtFixedRate(0, 1000) {
                invokeLater {
                    markdownViewer.component.revalidate()
                    markdownViewer.component.repaint()
                }
            }
        }
        preferredSize = java.awt.Dimension(800, 600)
        minimumSize = java.awt.Dimension(400, 300)
        pack()
        setLocationRelativeTo(null)
        layout = BorderLayout()
        add(scrollPane, BorderLayout.CENTER)

        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosed(e: WindowEvent?) {
                project.service<RunningCommandService>().removeRunningCommand(this@MarkdownDialog)
                super.windowClosed(e)

            }

            override fun windowClosing(windowEvent: java.awt.event.WindowEvent?) {
                if (isProcessFinished || onAbort == null) {
                    dispose()
                } else {
                    isProcessFinished = true  // Prevent multiple abort calls
                    onAbort.abortCommand()
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
                    onAbort.abortCommand()
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
                    onAbort.abortCommand()
                }
            }
        })

    }

    private var lastContent = ""

    fun updateProgress(output: String, message: String) {
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            try {
                val newContent = output.replace("\r\n", "\n")
                if (newContent != lastContent) {
                    lastContent = newContent

                    // Store scroll info before update
                    val scrollBar = scrollPane.verticalScrollBar
                    val currentValue = scrollBar.value
                    val wasAtBottom = autoScroll

                    // Update content
                    markdownViewer.setMarkdownContent(newContent)
                    title = message

                    // Handle scrolling after content update
                    SwingUtilities.invokeLater {
                        if (wasAtBottom || autoScroll) {
                            // Scroll to bottom if auto-scroll is enabled or was at bottom
                            scrollBar.value = scrollBar.maximum
                        } else {
                            // Try to maintain previous scroll position
                            scrollBar.value = currentValue
                        }
                        // Ensure the scroll pane is properly laid out
                        scrollPane.revalidate()
                        scrollPane.repaint()
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
                    dispose()
                }
            }
        }
    }

    private fun cancelAutoClose() {
        autoCloseTimer?.cancel()
        keepOpenButton.isVisible = false
        title = initialTitle
    }

    fun setProcessFinished() {
        isProcessFinished = true
        invokeLater {
            closeButton.text = "Close"
            closeButton.mnemonic = KeyEvent.VK_C
            closeAndContinueButton.isVisible = true
        }
    }

    fun focus(delay: Long = 100) {
        Timer().schedule(delay) {
            SwingUtilities.invokeLater {
                toFront()
                requestFocus()
                markdownViewer.component.requestFocusInWindow()
            }
        }
    }

}
