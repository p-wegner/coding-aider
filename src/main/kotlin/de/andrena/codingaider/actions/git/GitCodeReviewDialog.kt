package de.andrena.codingaider.actions.git

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
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
import java.awt.event.KeyEvent
import javax.swing.*

class GitCodeReviewDialog(
    private val project: Project,
    private val preselectedBaseRef: String? = null,
    private val preselectedTargetRef: String? = null
) : DialogWrapper(project) {
    private val baseRefComboBox = GitRefComboBox(project)
    private val targetRefComboBox = GitRefComboBox(project)
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

    override fun createActions(): Array<Action> {
        val actions = super.createActions()
        (actions[0] as? DialogWrapperAction)?.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_O)
        (actions[1] as? DialogWrapperAction)?.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C)
        return actions
    }

    init {
        title = "Git Code Review"
        // Set preselected values if provided
        if (preselectedBaseRef != null) {
            baseRefComboBox.setText(preselectedBaseRef)
        }
        
        if (preselectedTargetRef != null) {
            targetRefComboBox.setText(preselectedTargetRef)
        }
        
        init()
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

        gbc.apply {
            gridx = 0; gridy = 1
            weightx = 0.0
        }
        refSelectionPanel.add(JLabel("Ref:"), gbc)
        
        gbc.apply {
            gridx = 1; gridy = 1
            weightx = 0.7
        }
        refSelectionPanel.add(baseRefComboBox.getComponent(), gbc)

        // Target Reference
        gbc.apply {
            gridx = 0; gridy = 2
            weightx = 0.0
        }

        gbc.apply {
            gridx = 0; gridy = 3
            weightx = 0.0
        }
        refSelectionPanel.add(JLabel("Ref:"), gbc)
        
        gbc.apply {
            gridx = 1; gridy = 3
            weightx = 0.7
        }
        refSelectionPanel.add(targetRefComboBox.getComponent(), gbc)

        // Help text
        gbc.apply {
            gridx = 0; gridy = 2
            gridwidth = 4
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
        }
        refSelectionPanel.add(JLabel("<html><i>Enter any git ref, i.e. a branch name, tag or commit hash</i></html>"), gbc)

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
