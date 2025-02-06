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
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
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
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        panel.preferredSize = Dimension(900, 700)
        
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = com.intellij.util.ui.JBUI.insets(5)
        }

        // Reference Selection Section
        val refSelectionPanel = JPanel(GridBagLayout()).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Git References"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            )
        }
        
        // Base Reference
        gbc.apply {
            gridx = 0; gridy = 0
            weightx = 0.0; anchor = GridBagConstraints.LINE_START
        }
        refSelectionPanel.add(JLabel("Base:"), gbc)
        
        gbc.apply {
            gridx = 1; weightx = 0.3
        }
        refSelectionPanel.add(baseRefTypeCombo, gbc)
        
        gbc.apply {
            gridx = 2; weightx = 0.7
        }
        refSelectionPanel.add(baseRefComboBox.getComponent(), gbc)

        // Target Reference
        gbc.apply {
            gridx = 0; gridy = 1
            weightx = 0.0
        }
        refSelectionPanel.add(JLabel("Target:"), gbc)
        
        gbc.apply {
            gridx = 1; weightx = 0.3
        }
        refSelectionPanel.add(targetRefTypeCombo, gbc)
        
        gbc.apply {
            gridx = 2; weightx = 0.7
        }
        refSelectionPanel.add(targetRefComboBox.getComponent(), gbc)

        // Add Reference Selection Panel to main panel
        gbc.apply {
            gridx = 0; gridy = 0
            weightx = 1.0; weighty = 0.0
            fill = GridBagConstraints.HORIZONTAL
        }
        panel.add(refSelectionPanel, gbc)

        // Prompt Section
        val promptPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Review Focus Areas"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            )
        }
        
        promptArea.font = UIManager.getFont("TextField.font")
        promptPanel.add(JBScrollPane(promptArea), BorderLayout.CENTER)

        gbc.apply {
            gridy = 1
            weighty = 1.0
            fill = GridBagConstraints.BOTH
            insets = com.intellij.util.ui.JBUI.insets(10, 5, 5, 5)
        }
        panel.add(promptPanel, gbc)

        return panel
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
