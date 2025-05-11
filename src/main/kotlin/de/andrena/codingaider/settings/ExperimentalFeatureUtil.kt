package de.andrena.codingaider.settings

import javax.swing.JCheckBox

/**
 * Utility class for handling experimental features in the UI
 */
object ExperimentalFeatureUtil {
    /**
     * Mark a checkbox as an experimental feature
     * 
     * @param checkbox The checkbox to mark
     * @param originalText The original text of the checkbox (without the experimental marker)
     */
    fun markAsExperimental(checkbox: JCheckBox, originalText: String? = null) {
        val baseText = originalText ?: checkbox.text
        checkbox.text = "$baseText <span style='color:#FF6B00;'>[Experimental]</span>"
    }
    
    /**
     * The HTML color code for experimental feature labels
     */
    const val EXPERIMENTAL_COLOR = "#FF6B00"
}
