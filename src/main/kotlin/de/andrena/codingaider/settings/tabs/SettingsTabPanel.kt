package de.andrena.codingaider.settings.tabs

import com.intellij.openapi.Disposable
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.tabs.TabInfo
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.ApiKeyChecker

/**
 * Base class for settings tab panels
 */
abstract class SettingsTabPanel(
    protected val apiKeyChecker: ApiKeyChecker,
    protected val settings: AiderSettings = AiderSettings.getInstance()
) {
    /**
     * Create the panel content
     */
    abstract fun createPanel(panel: Panel)

    /**
     * Apply settings from UI components to the settings instance
     */
    abstract fun apply()

    /**
     * Reset UI components to match current settings
     */
    abstract fun reset()

    /**
     * Check if any settings have been modified
     */
    abstract fun isModified(): Boolean

    /**
     * Create a TabInfo with the panel content
     */
    fun createTabInfo(): TabInfo {
        val panel = com.intellij.ui.dsl.builder.panel {
            createPanel(this)
        }

        return TabInfo(panel).apply {
            setText(getTabName())
            setTooltipText(getTabTooltip())
        }
    }

    /**
     * Get the name of the tab
     */
    abstract fun getTabName(): String

    /**
     * Get the tooltip for the tab
     */
    abstract fun getTabTooltip(): String
    
    /**
     * Dispose any resources
     */
    open fun dispose() {
        // Default implementation does nothing
    }
}
