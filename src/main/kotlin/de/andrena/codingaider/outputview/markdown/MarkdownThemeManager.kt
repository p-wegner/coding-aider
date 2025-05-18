package de.andrena.codingaider.outputview.markdown

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages theme state for markdown rendering, detecting IDE theme changes
 * and providing theme information to renderers.
 */
class MarkdownThemeManager {
    private val LOG = Logger.getInstance(MarkdownThemeManager::class.java)
    private val darkThemeState = AtomicBoolean(JBColor.isBright().not())
    private val themeChangeListeners = CopyOnWriteArrayList<Pair<Disposable?, (Boolean) -> Unit>>()
    private var isInitialized = false
    private var messageBusConnection = ApplicationManager.getApplication().messageBus.connect()

    init {
        // Initialize with current theme
        darkThemeState.set(JBColor.isBright().not())
        
        // Listen for theme changes
        try {
            messageBusConnection.subscribe(LafManagerListener.TOPIC, LafManagerListener {
                val isDark = JBColor.isBright().not()
                if (darkThemeState.get() != isDark) {
                    darkThemeState.set(isDark)
                    notifyThemeChangeListeners()
                }
            })
            isInitialized = true
        } catch (e: Exception) {
            // Fallback if we can't subscribe to theme changes
            LOG.warn("Could not subscribe to theme changes", e)
        }
        
        // Register self-cleanup when application is disposed
        Disposer.register(ApplicationManager.getApplication(), Disposable {
            cleanup()
        })
    }

    /**
     * Updates the current theme and notifies listeners if changed
     * @return true if theme was changed, false otherwise
     */
    fun updateTheme(darkTheme: Boolean): Boolean {
        val changed = darkThemeState.getAndSet(darkTheme) != darkTheme
        if (changed) {
            notifyThemeChangeListeners()
        }
        return changed
    }

    /**
     * Current dark theme state
     */
    val isDarkTheme: Boolean
        get() = darkThemeState.get()

    /**
     * Add a listener to be notified of theme changes
     * @param parentDisposable Optional disposable that will automatically remove the listener when disposed
     * @param listener The listener function to call when theme changes
     */
    fun addThemeChangeListener(parentDisposable: Disposable? = null, listener: (Boolean) -> Unit) {
        themeChangeListeners.add(Pair(parentDisposable, listener))
        
        // Register automatic cleanup when parent is disposed
        if (parentDisposable != null) {
            Disposer.register(parentDisposable, Disposable {
                themeChangeListeners.removeIf { it.first == parentDisposable }
            })
        }
    }

    /**
     * Remove a previously added theme change listener
     */
    fun removeThemeChangeListener(listener: (Boolean) -> Unit) {
        themeChangeListeners.removeIf { it.second == listener }
    }

    private fun notifyThemeChangeListeners() {
        val isDark = darkThemeState.get()
        themeChangeListeners.forEach { it.second(isDark) }
    }
    
    private fun cleanup() {
        try {
            messageBusConnection.disconnect()
            themeChangeListeners.clear()
        } catch (e: Exception) {
            LOG.warn("Error during MarkdownThemeManager cleanup", e)
        }
    }
}
