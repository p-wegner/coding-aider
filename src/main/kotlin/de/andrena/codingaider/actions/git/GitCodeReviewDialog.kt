package de.andrena.codingaider.actions.git

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.VcsException
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.utils.GitDiffUtils
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

class GitCodeReviewDialog(private val project: Project) : DialogWrapper(project) {
    private val baseRefComboBox = GitRefComboBox(project)
    private val targetRefComboBox = GitRefComboBox(project)
    private val baseRefTypeCombo = ComboBox(GitRefComboBox.RefType.values())
    private val targetRefTypeCombo = ComboBox(GitRefComboBox.RefType.values())
    
    private val promptArea = JBTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
        text = """Review focusing on:
            |1. Code quality and best practices
            |2. Potential bugs or issues
            |3. Performance implications
            |4. Security considerations
            |5. Design patterns and architecture
            |6. Test coverage
            |7. Documentation needs""".trimMargin()
    }
    
    private var selectedFiles: List<FileData> = emptyList()
    private var baseCommit: String = ""

    init {
        title = "Git Code Review"
        setupRefTypeListeners()
        init()
    }

    private fun setupRefTypeListeners() {
        baseRefTypeCombo.addActionListener {
            baseRefComboBox.setMode(baseRefTypeCombo.selectedItem as GitRefComboBox.RefType)
        }
        targetRefTypeCombo.addActionListener {
            targetRefComboBox.setMode(targetRefTypeCombo.selectedItem as GitRefComboBox.RefType)
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(10, 10))
        panel.preferredSize = Dimension(800, 600)

        val inputPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            
            // Base ref selection
            add(createRefSelectionPanel("Base:", baseRefTypeCombo, baseRefComboBox))
            add(Box.createVerticalStrut(10))
            
            // Target ref selection
            add(createRefSelectionPanel("Target:", targetRefTypeCombo, targetRefComboBox))
            add(Box.createVerticalStrut(10))
            
            // Prompt area
            add(JLabel("Review Prompt:"))
            add(JBScrollPane(promptArea).apply {
                preferredSize = Dimension(750, 400)
            })
        }

        panel.add(inputPanel, BorderLayout.CENTER)
        return panel
    }

    private fun createRefSelectionPanel(
        label: String, 
        typeCombo: JComboBox<GitRefComboBox.RefType>,
        refCombo: GitRefComboBox
    ): JPanel {
        return JPanel(BorderLayout(5, 0)).apply {
            add(JLabel(label), BorderLayout.WEST)
            add(typeCombo, BorderLayout.CENTER)
            add(refCombo.getComponent(), BorderLayout.EAST)
        }
    }

    override fun doOKAction() {
        baseCommit = baseRefComboBox.getText().trim()
        val targetCommit = targetRefComboBox.getText().trim()

        if (baseCommit.isBlank() || targetCommit.isBlank()) {
            Messages.showErrorDialog(
                project,
                "Both base and target refs must be specified",
                "Invalid Input"
            )
            return
        }

        try {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(
                {
                    selectedFiles = GitDiffUtils.getChangedFiles(project, baseCommit, targetCommit).files
                },
                "Getting Changed Files",
                true,
                project
            )
            super.doOKAction()
        } catch (e: VcsException) {
            Messages.showErrorDialog(
                project,
                e.message ?: "Failed to get changed files",
                "Git Error"
            )
            return
        }
    }

    fun getPrompt(): String = promptArea.text.trim()

    fun getSelectedRefs(): Pair<String, String> =
        Pair(baseRefComboBox.getText().trim(), targetRefComboBox.getText().trim())
}
