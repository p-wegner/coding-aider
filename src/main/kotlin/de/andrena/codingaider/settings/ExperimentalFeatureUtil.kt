package de.andrena.codingaider.settings

import javax.swing.JCheckBox

object ExperimentalFeatureUtil {
    fun markAsExperimental(checkbox: JCheckBox, originalText: String? = null) {
        val baseText = originalText ?: checkbox.text
        checkbox.text = "$baseText [Experimental]"
    }
    
}
