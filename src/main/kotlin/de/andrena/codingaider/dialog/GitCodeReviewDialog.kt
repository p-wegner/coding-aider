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
        baseCommit = baseCommitField.text.trim()
        val targetCommit = targetCommitField.text.trim()
        
        if (baseCommit.isBlank() || targetCommit.isBlank()) {
            Messages.showErrorDialog(
                project,
                "Both base and target commits/branches must be specified",
                "Invalid Input"
            )
            return
        }

        try {
            selectedFiles = GitDiffUtils.getChangedFiles(project, baseCommit, targetCommit)
            super.doOKAction()
        } catch (e: VcsException) {
            Messages.showErrorDialog(
                project,
                e.message ?: "Failed to get changed files",
                "Git Error"
            )
        }
    }

    fun getPrompt(): String = promptArea.text.ifBlank {
        """
        Review focusing on:
        1. Code quality and best practices
        2. Potential bugs or issues
        3. Performance implications
        4. Security considerations
        """.trimIndent()
    }

    fun getSelectedRefs(): Pair<String, String> =
        Pair(baseCommitField.text.trim(), targetCommitField.text.trim())
}
