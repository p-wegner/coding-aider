package de.andrena.codingaider.utils

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.ui.components.panels.Wrapper
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.reflect.KMutableProperty0

class CollapsiblePanel(
    name: String,
    private val isCollapsedProperty: KMutableProperty0<Boolean>,
    private val content: JComponent
) {
    private val wrapper = Wrapper().apply {
        setContent(content)
        isVisible = true
        preferredSize = if (!isCollapsedProperty.get()) null else Dimension(0, 0)
    }
    
    private val animation = PanelAnimation(wrapper)
    
    val headerPanel = JPanel(BorderLayout()).apply {
        add(createCollapseButton(name), BorderLayout.WEST)
        add(JLabel(name), BorderLayout.CENTER)
    }
    
    val contentPanel = wrapper

    private fun createCollapseButton(name: String): ActionButton {
        val presentation = Presentation().apply {
            icon = if (isCollapsedProperty.get()) AllIcons.General.ArrowRight else AllIcons.General.ArrowDown
            text = "Toggle $name"
            description = "Show/hide $name panel"
        }
        
        return ActionButton(
            object : AnAction() {
                override fun actionPerformed(e: AnActionEvent) {
                    isCollapsedProperty.set(!isCollapsedProperty.get())
                    val isCollapsed = isCollapsedProperty.get()
                    
                    presentation.icon = if (isCollapsed) AllIcons.General.ArrowRight else AllIcons.General.ArrowDown
                    
                    val startHeight = if (isCollapsed) content.preferredSize.height else 0
                    val endHeight = if (isCollapsed) 0 else content.preferredSize.height
                    
                    animation.animate(startHeight, endHeight) {
                        wrapper.preferredSize = if (isCollapsed) Dimension(0, 0) else null
                        wrapper.revalidate()
                        wrapper.repaint()
                    }
                }
            },
            presentation,
            "Aider${name}Button",
            ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
        )
    }
}
