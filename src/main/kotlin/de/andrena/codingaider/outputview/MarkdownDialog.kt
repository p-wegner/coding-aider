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
    private val scrollPane = JBScrollPane().apply {
        setViewportView(markdownViewer.component)
        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
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
    private var autoScroll = true

    init {
        title = initialTitle
        markdownViewer.setMarkdown(initialText)

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

                    // Calculate if we're at bottom before update
                    val scrollBar = scrollPane.verticalScrollBar
                    val extent = scrollBar.model.extent
                    val maximum = scrollBar.model.maximum
                    val currentValue = scrollBar.value
                    val isAtBottom = (currentValue + extent) >= (maximum - 20)
                    
                    // Store viewport position relative to total height
                    val viewportHeight = scrollPane.viewport.height
                    val totalHeight = scrollPane.viewport.view?.height ?: 0
                    val scrollRatio = if (totalHeight > 0) currentValue.toDouble() / totalHeight else 0.0

                    // Update content
                    markdownViewer.setMarkdown(newContent)
                    title = message

                    // Handle scrolling after content update
                    SwingUtilities.invokeLater {
                        scrollPane.revalidate()
                        val newTotalHeight = scrollPane.viewport.view?.height ?: 0
                        
                        if (autoScroll || isAtBottom) {
                            // Scroll to bottom
                            scrollBar.value = scrollBar.maximum - extent
                        } else {
                            // Maintain relative scroll position
                            val newScrollValue = (scrollRatio * newTotalHeight).toInt()
                            scrollBar.value = newScrollValue.coerceIn(0, scrollBar.maximum - extent)
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
                val themeCss = if (isDark) {
                    """
                    body { background: #2b2b2b; color: #ffffff; }
                    .aider-intention { background: #1a2733; border-color: #2c4356; color: #589df6; }
                    .aider-summary { background: #2b2b2b; border-color: #404040; color: #cccccc; }
                    """
                } else {
                    """
                    body { background: #ffffff; color: #000000; }
                    .aider-intention { background: #f0f7ff; border-color: #bcd6f5; color: #0066cc; }
                    .aider-summary { background: #f7f7f7; border-color: #e0e0e0; color: #333333; }
                    """
                }
                currentContent = lastContent
                markdownViewer.setMarkdown("""
                    <style>$themeCss</style>
                    $currentContent
                """.trimIndent())
            }
        }
    }

}
