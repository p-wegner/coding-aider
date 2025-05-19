package de.andrena.codingaider.outputview.markdown

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.concurrency.AppExecutorUtil
import org.cef.browser.CefBrowser
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.BorderLayout
import java.awt.Dimension
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

/**
 * Markdown renderer implementation using JCEF (Chromium Embedded Framework)
 * for high-quality HTML rendering with JavaScript support.
 */
class JcefMarkdownRenderer(
    private val contentProcessor: MarkdownContentProcessor,
    private val themeManager: MarkdownThemeManager
) : MarkdownRenderer {
    private val LOG = Logger.getInstance(JcefMarkdownRenderer::class.java)
    private val isDisposed = AtomicBoolean(false)
    private val browser: JBCefBrowser? = createBrowserSafely()
    private val mainPanel = JPanel(BorderLayout())
    private val loadCompleted = AtomicBoolean(false)
    private val pendingContent = AtomicReference<String?>(null)
    private val parentDisposable = Disposer.newDisposable("JcefMarkdownRenderer")

    private var currentContent = ""
    private var initializationFuture: CompletableFuture<Boolean>? = null

    /**
     * Creates a JCEF browser instance safely with exception handling
     */
    private fun createBrowserSafely(): JBCefBrowser? {
        return try {
            JBCefBrowser()
        } catch (e: Exception) {
            LOG.error("Failed to create JCEF browser", e)
            null
        }
    }

    override val component: JComponent
        get() = mainPanel

    override val isReady: Boolean
        get() = loadCompleted.get() && !isDisposed.get()

    init {
        // Configure browser and panel if browser was created successfully
        if (browser != null) {
            browser.component.minimumSize = Dimension(200, 100)
            browser.component.preferredSize = Dimension(600, 400)
            mainPanel.add(browser.component, BorderLayout.CENTER)

            // Register browser with parent disposable
            Disposer.register(parentDisposable, browser)
        } else {
            // Add a fallback component if browser creation failed
            val fallbackLabel = JPanel().apply {
                minimumSize = Dimension(200, 100)
                preferredSize = Dimension(600, 400)
                layout = BorderLayout()
                add(
                    JLabel("JCEF browser initialization failed. Using fallback renderer.", SwingConstants.CENTER),
                    BorderLayout.CENTER
                )
            }
            mainPanel.add(fallbackLabel, BorderLayout.CENTER)

            // Mark as completed since we're using fallback
            loadCompleted.set(true)
        }

        // Listen for theme changes
        themeManager.addThemeChangeListener(parentDisposable) { isDark ->
            if (!isDisposed.get()) {
                SwingUtilities.invokeLater {
                    setDarkTheme(isDark)
                }
            }
        }

        // Initialize browser with HTML template if browser was created successfully
        if (browser != null) {
            initializeBrowser()

            // Set up load handler to detect when browser is ready
            val loadHandler = object : CefLoadHandlerAdapter() {
                override fun onLoadingStateChange(
                    cefBrowser: CefBrowser,
                    isLoading: Boolean,
                    canGoBack: Boolean,
                    canGoForward: Boolean
                ) {
                    if (!isLoading) {
                        // Browser is ready, apply any pending content
                        SwingUtilities.invokeLater {
                            if (loadCompleted.compareAndSet(false, true)) {
                                pendingContent.getAndSet(null)?.let { content ->
                                    updateContent(content)
                                }
                                initializationFuture?.complete(true)
                            }
                        }
                    }
                }
            }
            browser.jbCefClient.addLoadHandler(loadHandler, browser.cefBrowser)

            // Register load handler with parent disposable
            Disposer.register(parentDisposable, Disposable {
                try {
                    browser.jbCefClient.removeLoadHandler(loadHandler, browser.cefBrowser)
                } catch (e: Exception) {
                    LOG.warn("Error removing load handler", e)
                }
            })
        }

        // Set up initialization future with timeout
        initializationFuture = CompletableFuture<Boolean>().completeOnTimeout(true, 2, TimeUnit.SECONDS)
        initializationFuture?.thenAcceptAsync({
            SwingUtilities.invokeLater {
                if (loadCompleted.compareAndSet(false, true)) {
                    pendingContent.getAndSet(null)?.let { content ->
                        updateContent(content)
                    }
                }
            }
        }, AppExecutorUtil.getAppExecutorService())
    }

    private fun initializeBrowser() {
        if (browser == null) return

        try {
            // Load HTML template from resources
            val htmlTemplate = loadResourceAsString("/html/markdown-template.html")

            // Replace placeholders with actual values
            val processedTemplate = htmlTemplate
                .replace("\${BACKGROUND_COLOR}", if (themeManager.isDarkTheme) "#2b2b2b" else "#ffffff")
                .replace("\${TEXT_COLOR}", if (themeManager.isDarkTheme) "#ffffff" else "#000000")
                .replace("\${SCRIPT_PATH}", getScriptUrl())

            // Load the processed template
            val dataUrl = "data:text/html;charset=utf-8;base64," +
                    Base64.getEncoder().encodeToString(processedTemplate.toByteArray(StandardCharsets.UTF_8))
            browser.loadURL(dataUrl)
        } catch (e: Exception) {
            LOG.error("Error initializing browser with template", e)
            // Fallback to minimal HTML if template loading fails
            val fallbackHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Markdown Viewer (Fallback)</title>
                    <style>
                        body { 
                            font-family: sans-serif; 
                            margin: 20px;
                            background-color: ${if (themeManager.isDarkTheme) "#2b2b2b" else "#ffffff"};
                            color: ${if (themeManager.isDarkTheme) "#ffffff" else "#000000"};
                        }
                    </style>
                </head>
                <body>
                    <div id="content"></div>
                    <script>
                        function updateContent(html) {
                            document.getElementById('content').innerHTML = html;
                        }
                        function scrollToBottom() {
                            window.scrollTo(0, document.body.scrollHeight);
                        }
                    </script>
                </body>
                </html>
            """.trimIndent()

            val fallbackUrl = "data:text/html;charset=utf-8;base64," +
                    Base64.getEncoder().encodeToString(fallbackHtml.toByteArray(StandardCharsets.UTF_8))
            browser?.loadURL(fallbackUrl)
        }
    }

    override fun setMarkdown(markdown: String) {
        if (isDisposed.get()) return

        if (!loadCompleted.get()) {
            // Browser not ready yet, store content for later
            pendingContent.set(markdown)
            // Store content for processing (only after we've saved the pending content)
            currentContent = markdown
            return
        }

        // Process markdown in background thread
        AppExecutorUtil.getAppExecutorService().submit {
            try {
                if (isDisposed.get()) return@submit

                // Store content for processing
                currentContent = markdown

                // Process markdown to HTML
                val html = contentProcessor.processMarkdown(markdown, themeManager.isDarkTheme)

                // Update UI on EDT
                SwingUtilities.invokeLater {
                    if (!isDisposed.get()) {
                        updateContentInBrowser(html)
                    }
                }
            } catch (e: Exception) {
                LOG.error("Error processing markdown", e)
            }
        }
    }

    private fun updateContent(html: String) {
        updateContentInBrowser(html)
    }

    private fun updateContentInBrowser(html: String) {
        if (isDisposed.get()) return

        // If browser is null, we can't update content
        if (browser == null) {
            LOG.warn("Cannot update content: browser is null")
            return
        }

        try {
            // Execute JavaScript to update content
            val script = """
                try {
                    if (typeof updateContent === 'function') {
                        updateContent(${jsonEscapeString(html)});
                    } else {
                        // Basic fallback
                        document.getElementById('content').innerHTML = ${jsonEscapeString(html)};
                    }
                } catch (e) {
                    console.error("Error updating content:", e);
                    // Ultra basic fallback
                    document.getElementById('content').innerHTML = ${jsonEscapeString(html)};
                }
            """.trimIndent()

            executeJavaScript(script)
        } catch (e: Exception) {
            LOG.error("Error updating content in browser", e)
        }
    }

    override fun setDarkTheme(isDarkTheme: Boolean) {
        if (isDisposed.get()) return

        if (themeManager.updateTheme(isDarkTheme) && browser != null) {
            // Apply theme change via CSS variables instead of full rerender
            executeJavaScript(
                """
                (function() {
                    // Update theme colors
                    document.body.style.backgroundColor = '${if (isDarkTheme) "#2b2b2b" else "#ffffff"}';
                    document.body.style.color = '${if (isDarkTheme) "#ffffff" else "#000000"}';
                    
                    // Add/remove theme class
                    if (${isDarkTheme}) {
                        document.body.classList.add('dark-theme');
                        document.body.classList.remove('light-theme');
                    } else {
                        document.body.classList.add('light-theme');
                        document.body.classList.remove('dark-theme');
                    }
                    
                    // Update code blocks and other themed elements without full re-render
                    document.querySelectorAll('.collapsible-panel').forEach(panel => {
                        panel.style.borderColor = '${if (isDarkTheme) "rgba(200, 200, 200, 0.2)" else "rgba(0, 0, 0, 0.2)"}';
                    });
                    
                    document.querySelectorAll('.collapsible-header').forEach(header => {
                        header.style.backgroundColor = '${if (isDarkTheme) "rgba(200, 200, 200, 0.1)" else "rgba(0, 0, 0, 0.1)"}';
                    });
                })();
            """.trimIndent()
            )
        }
    }

    override fun scrollToBottom() {
        if (isDisposed.get() || !loadCompleted.get() || browser == null) return

        try {
            // First immediate scroll attempt
            executeJavaScript("scrollToBottom();")

            // Single delayed scroll attempt as backup
            // This helps with cases where content is still being rendered
            if (!isDisposed.get()) {
                AppExecutorUtil.getAppScheduledExecutorService().schedule({
                    if (!isDisposed.get()) {
                        executeJavaScript("scrollToBottom();")
                    }
                }, 300, TimeUnit.MILLISECONDS)
            }
        } catch (e: Exception) {
            LOG.error("Error scrolling to bottom", e)
        }
    }


    private fun executeJavaScript(script: String) {
        if (isDisposed.get() || browser == null) return

        try {
            // Check if we're already on EDT
            if (SwingUtilities.isEventDispatchThread()) {
                if (!isDisposed.get()) {
                    browser.cefBrowser.executeJavaScript(script, browser.cefBrowser.url, 0)
                }
            } else {
                // Only use invokeLater if we're not already on EDT
                SwingUtilities.invokeLater {
                    if (!isDisposed.get()) {
                        browser.cefBrowser.executeJavaScript(script, browser.cefBrowser.url, 0)
                    }
                }
            }
        } catch (e: Exception) {
            LOG.error("Error executing JavaScript", e)
        }
    }

    /**
     * Gets the URL for the JavaScript file, either from resources or as a data URL
     */
    private fun getScriptUrl(): String {
        try {
            // Try to get the script as a resource URL
            val scriptUrl = javaClass.getResource("/js/markdown-viewer.js")
            if (scriptUrl != null) {
                return scriptUrl.toString()
            }
        } catch (e: Exception) {
            LOG.warn("Could not load script as resource URL", e)
        }

        // Fallback: inline the script as a data URL
        try {
            val scriptContent = loadResourceAsString("/js/markdown-viewer.js")
            return "data:text/javascript;charset=utf-8;base64," +
                    Base64.getEncoder().encodeToString(scriptContent.toByteArray(StandardCharsets.UTF_8))
        } catch (e: Exception) {
            LOG.error("Could not load script content", e)
            // Return minimal script as data URL
            val minimalScript = "function updateContent(html){document.getElementById('content').innerHTML=html;}" +
                    "function scrollToBottom(){window.scrollTo(0,document.body.scrollHeight);}"
            return "data:text/javascript;charset=utf-8;base64," +
                    Base64.getEncoder().encodeToString(minimalScript.toByteArray(StandardCharsets.UTF_8))
        }
    }

    /**
     * Loads a resource file as a string
     */
    private fun loadResourceAsString(resourcePath: String): String {
        val inputStream = javaClass.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Resource not found: $resourcePath")

        return inputStream.use { stream ->
            stream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                reader.readText()
            }
        }
    }

    /**
     * Properly escapes a string for use in JavaScript by converting it to a JSON string
     */
    private fun jsonEscapeString(str: String): String {
        // Create a StringBuilder with estimated capacity
        val sb = StringBuilder(str.length + 10)
        sb.append('"')

        // Manually escape all special characters
        for (c in str) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '\"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f") // Form feed
                '<' -> {
                    // Special handling for </script> to prevent XSS
                    if (str.indexOf("</script>", sb.length - 1) == sb.length - 1) {
                        sb.append("<\\/")
                    } else {
                        sb.append(c)
                    }
                }

                in '\u0000'..'\u001F' -> {
                    // Control characters need unicode escape
                    sb.append("\\u")
                    sb.append(String.format("%04x", c.code))
                }

                else -> sb.append(c)
            }
        }

        sb.append('"')
        return sb.toString()
    }

    override fun dispose() {
        // Only dispose once
        if (isDisposed.getAndSet(true)) return

        try {
            // Cancel any pending initialization
            initializationFuture?.cancel(true)

            // Ensure we're on EDT for Swing operations
            if (!SwingUtilities.isEventDispatchThread()) {
                try {
                    SwingUtilities.invokeAndWait { disposeOnEDT() }
                } catch (e: Exception) {
                    LOG.error("Error during invokeAndWait in dispose", e)
                    // Try to dispose anyway as a fallback
                    disposeOnEDT()
                }
            } else {
                disposeOnEDT()
            }
        } catch (e: Exception) {
            LOG.error("Error disposing JcefMarkdownRenderer", e)
        }
    }

    override fun supportsDevTools(): Boolean {
        // DevTools are supported if we have a valid browser instance
        return browser != null && !isDisposed.get()
    }

    override fun showDevTools(): Boolean {
        if (!supportsDevTools()) {
            LOG.warn("DevTools not supported: browser is null or disposed")
            return false
        }

        try {
            browser!!.openDevtools()
            return true
        } catch (e: Exception) {
            LOG.error("Error opening DevTools", e)
            return false
        }
    }

    private fun disposeOnEDT() {
        try {
            // Dispose the parent disposable which will clean up all registered resources
            // This will also clean up the theme change listener and load handler
            Disposer.dispose(parentDisposable)

            // Clear panel
            mainPanel.removeAll()
        } catch (e: Exception) {
            LOG.error("Error in disposeOnEDT", e)
        }
    }
}
