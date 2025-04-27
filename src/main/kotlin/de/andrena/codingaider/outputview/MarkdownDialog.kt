package de.andrena.codingaider.outputview

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
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
        verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
    }
    private var autoCloseTimer: Timer? = null
    private var autoCloseTask: TimerTask? = null
    // Removed unused refreshTimer property (High Priority Issue #5)
    private var keepOpenButton: JButton
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
    // Removed unused resizeTimer property (High Priority Issue #4)

    init {
        title = initialTitle

        // Add resize listener with debouncing using Swing timer
        addComponentListener(object : java.awt.event.ComponentAdapter() {
            private val resizeSwingTimer = javax.swing.Timer(200) {
                markdownViewer.component.revalidate()
                markdownViewer.component.repaint()
                scrollPane.revalidate()
                scrollPane.repaint()
                // Ensure content is displayed after resize
                markdownViewer.ensureContentDisplayed()
            }.apply {
                isRepeats = false
            }

            override fun componentResized(e: java.awt.event.ComponentEvent) {
                resizeSwingTimer.restart()
            }

            // Also handle when the component is shown
            override fun componentShown(e: java.awt.event.ComponentEvent) {
                // Ensure content is displayed when dialog becomes visible
                invokeLater {
                    markdownViewer.ensureContentDisplayed()

                    // Schedule another check after a short delay to catch any initialization issues
                    javax.swing.Timer(500) {
                        markdownViewer.ensureContentDisplayed()
                    }.apply {
                        isRepeats = false
                        start()
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
        // Add content panel with proper weighting
        val contentPanel = JPanel(BorderLayout(0, 0)).apply {
            border = javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5)
            add(scrollPane, BorderLayout.CENTER)
        }
        add(contentPanel, BorderLayout.CENTER)

        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosed(e: WindowEvent?) {
                try {
                    // Clean up all timers
                    // Removed refreshTimer and resizeTimer from cleanup (High Priority Issues #4 & #5)
                    listOf(autoCloseTimer).forEach { timer ->
                        timer?.cancel()
                        timer?.purge()
                    }
                    autoCloseTask?.cancel()

                    // Clear references
                    autoCloseTimer = null
                    autoCloseTask = null

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

        // Remove duplicate window listener - already defined above
        markdownViewer.setMarkdown(initialText)
        positionOnSameScreen()
    }

    private var lastContent = ""

    fun updateProgress(output: String, title: String) {
        ApplicationManager.getApplication().invokeLater {
            // Check if dialog is still displayable before updating UI
            if (!isDisplayable) return@invokeLater

            // Check if content actually changed
            val normalizedOutput = output.replace("\r\n", "\n")
            // Removed the check against lastContent here, as the check is now inside MarkdownJcefViewer.updateContent
            // if (normalizedOutput == lastContent) return@invokeLater

            lastContent = normalizedOutput // Keep lastContent updated for potential future use or debugging
            markdownViewer.setMarkdown(lastContent)
            this@MarkdownDialog.title = title
        }
    }
    fun startAutoCloseTimer(autocloseDelay: Int) {
        val settings = getInstance()
        if (!settings.enableMarkdownDialogAutoclose) return

        // Cancel any existing timer
        autoCloseTimer?.cancel()
        autoCloseTask?.cancel()

        // Create a new daemon timer
        val timer = Timer(true)
        autoCloseTimer = timer

        // Make the keep open button visible
        SwingUtilities.invokeLater {
            keepOpenButton.isVisible = true
        }

        // Start with the correct value
        var remainingSeconds = max(1, autocloseDelay)

        autoCloseTask = object : TimerTask() {
            override fun run() {
                SwingUtilities.invokeLater {
                    if (!isDisplayable) {
                        // Dialog already disposed, cancel timer
                        timer.cancel()
                        return@invokeLater
                    }

                    // Update UI first, then decrement
                    title = "$initialTitle - Closing in $remainingSeconds seconds"

                    remainingSeconds--

                    if (remainingSeconds < 0) {
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
                            timer.cancel()
                            dispose()
                        }
                    }
                }
            }
        }

        // Schedule the timer task to start after 1 second (Critical Issue #2 fix)
        timer.scheduleAtFixedRate(autoCloseTask, 1000, 1000)
    }

    private fun cancelAutoClose() {
        autoCloseTimer?.cancel()
        autoCloseTimer?.purge()
        autoCloseTask?.cancel()
        autoCloseTimer = null
        autoCloseTask = null

        SwingUtilities.invokeLater {
            keepOpenButton.isVisible = false
            title = initialTitle
        }
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
        // Use Swing timer instead of java.util.Timer
        javax.swing.Timer(kotlin.math.min(delay, Int.MAX_VALUE.toLong()).toInt()) { // Applied Medium Issue #9 fix
            toFront()
            requestFocus()
            markdownViewer.component.requestFocusInWindow()
            // Set dark theme based on current IDE theme and ensure content is displayed
            markdownViewer.setDarkTheme(com.intellij.openapi.editor.colors.EditorColorsManager.getInstance().isDarkEditor)
            // No need to call setMarkdown here, ensureContentDisplayed handles it if needed
            markdownViewer.ensureContentDisplayed()
        }.apply {
            isRepeats = false
            start()
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
