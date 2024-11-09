package de.andrena.codingaider.utils

import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.Timer
import kotlin.math.roundToInt

class PanelAnimation(private val component: JComponent) {
    private var timer: Timer? = null
    
    fun animate(
        startHeight: Int,
        endHeight: Int,
        durationMs: Int = 200,
        onComplete: () -> Unit = {}
    ) {
        timer?.stop()
        
        var progress = 0f
        timer = Timer(16) { // ~60fps
            progress += 16f / durationMs
            if (progress >= 1f) {
                component.preferredSize = Dimension(component.width, endHeight)
                component.revalidate()
                component.repaint()
                timer?.stop()
                onComplete()
            } else {
                val currentHeight = lerp(startHeight, endHeight, easeInOut(progress))
                component.preferredSize = Dimension(component.width, currentHeight)
                component.revalidate()
                component.repaint()
            }
        }
        timer?.start()
    }
    
    private fun lerp(start: Int, end: Int, fraction: Float): Int {
        return (start + (end - start) * fraction).roundToInt()
    }
    
    private fun easeInOut(x: Float): Float {
        return when {
            x < 0.5f -> 2 * x * x
            else -> 1 - (-2 * x + 2).pow(2) / 2
        }
    }
    
    private fun Float.pow(n: Int): Float = Math.pow(this.toDouble(), n.toDouble()).toFloat()
}
