package de.andrena.codingaider.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.utils.GitDiffUtils
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

class GitCodeReviewDialog(private val project: Project) : DialogWrapper(project) {
    private val baseCommitField = JBTextField()
    private val targetCommitField = JBTextField()
    private val promptArea = JBTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
    }
    private var selectedFiles: List<FileData> = emptyList()
    private var baseCommit: String = ""

    init {
        title = "Git Code Review"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(10, 10))
        panel.preferredSize = Dimension(500, 300)

        val inputPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(createLabeledComponent("Base Branch/Commit:", baseCommitField))
            add(Box.createVerticalStrut(10))
            add(createLabeledComponent("Target Branch/Commit:", targetCommitField))
            add(Box.createVerticalStrut(10))
            add(JLabel("Review Prompt:"))
            add(JBScrollPane(promptArea))
        }

        panel.add(inputPanel, BorderLayout.CENTER)
        return panel
    }

    private fun createLabeledComponent(label: String, component: JComponent): JPanel {
        return JPanel(BorderLayout()).apply {
            add(JLabel(label), BorderLayout.WEST)
            add(component, BorderLayout.CENTER)
        }
    }

    override fun doOKAction() {
        baseCommit = baseCommitField.text
        val targetCommit = targetCommitField.text
        
        if (baseCommit.isBlank() || targetCommit.isBlank()) {
            return
        }

        selectedFiles = GitDiffUtils.getChangedFiles(project, baseCommit, targetCommit)
        super.doOKAction()
    }

    fun getPrompt(): String = """
        Review the code changes between commits/branches. Focus on:
        1. Code quality and best practices
        2. Potential bugs or issues
        3. Performance implications
        4. Security considerations
        
        Custom instructions: ${promptArea.text}
    """.trimIndent()

    fun getSelectedFiles(): List<FileData> = selectedFiles
    
    fun getBaseCommit(): String = baseCommit
}
