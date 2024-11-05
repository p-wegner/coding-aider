package de.andrena.codingaider.outputview

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.TextEditorWithPreviewProvider
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBScrollPane
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import org.intellij.plugins.markdown.lang.MarkdownFileType
import org.intellij.plugins.markdown.ui.preview.MarkdownPreviewFileEditor
import java.awt.BorderLayout
import java.awt.EventQueue.invokeLater
import java.awt.Frame
import java.awt.event.KeyEvent
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
    private val onAbort: Abortable? = null
) : JDialog(null as Frame?, false) {
    private val virtualFile = LightVirtualFile("preview.md", MarkdownFileType.INSTANCE, initialText.replace("\r\n", "\n"))
    private val document = FileDocumentManager.getInstance().getDocument(virtualFile)!!
    private val textArea: MarkdownPreviewFileEditor = MarkdownPreviewFileEditorUtil.createMarkdownPreviewEditor(project, virtualFile, document)

    private val scrollPane by lazy {
        JBScrollPane(textArea.component).apply {
            setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED)
            setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
            viewport.scrollMode = JViewport.BACKINGSTORE_SCROLL_MODE
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
    private var isProcessFinished = false

    init {
        title = initialTitle
        
        // Start refresh timer
        refreshTimer = Timer().apply {
            scheduleAtFixedRate(0, 1000) {
                invokeLater {
                    textArea.selectNotify()
                    textArea.component.revalidate()
                    textArea.component.repaint()
                }
            }
        }
        setSize(800, 800)
        setLocationRelativeTo(null)
        layout = BorderLayout()
        add(scrollPane, BorderLayout.CENTER)

        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        addWindowListener(object : java.awt.event.WindowAdapter() {
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

    fun updateProgress(output: String, message: String) {
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            try {
                com.intellij.openapi.application.WriteAction.runAndWait<Throwable> {
                    document.setText(output.replace("\r\n", "\n"))
                }
                
                title = message
                
                // Force preview refresh
                textArea.selectNotify()
                
                // Ensure UI updates happen on EDT
                SwingUtilities.invokeLater {
                    // Update preview
                    textArea.component.revalidate()
                    textArea.component.repaint()
                    
                    // Scroll to bottom
                    val scrollBar = scrollPane.verticalScrollBar
                    scrollBar.value = scrollBar.maximum
                    
                    // Final refresh
                    scrollPane.revalidate()
                    scrollPane.repaint()
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
        }
    }

    fun focus(delay: Long = 100) {
        Timer().schedule(delay) {
            SwingUtilities.invokeLater {
                toFront()
                requestFocus()
                textArea.component.requestFocusInWindow()
            }
        }
    }

}
