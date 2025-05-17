package de.andrena.codingaider.outputview.markdown

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.JBColor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages theme state for markdown rendering, detecting IDE theme changes
 * and providing theme information to renderers.
 */
class MarkdownThemeManager {
    private val darkThemeState = AtomicBoolean(JBColor.isBright().not())
    private val themeChangeListeners = mutableListOf<(Boolean) -> Unit>()
    private var isInitialized = false

    init {
        // Initialize with current theme
        darkThemeState.set(JBColor.isBright().not())
        
        // Listen for theme changes
        try {
            val connection = ApplicationManager.getApplication().messageBus.connect()
            connection.subscribe(LafManagerListener.TOPIC, LafManagerListener {
                val isDark = JBColor.isBright().not()
                if (darkThemeState.get() != isDark) {
                    darkThemeState.set(isDark)
                    notifyThemeChangeListeners()
                }
            })
            isInitialized = true
        } catch (e: Exception) {
            // Fallback if we can't subscribe to theme changes
            println("Warning: Could not subscribe to theme changes: ${e.message}")
        }
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
     */
    fun addThemeChangeListener(listener: (Boolean) -> Unit) {
        themeChangeListeners.add(listener)
    }

    /**
     * Remove a previously added theme change listener
     */
    fun removeThemeChangeListener(listener: (Boolean) -> Unit) {
        themeChangeListeners.remove(listener)
    }

    private fun notifyThemeChangeListeners() {
        val isDark = darkThemeState.get()
        themeChangeListeners.forEach { it(isDark) }
    }
}
