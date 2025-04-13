package de.andrena.codingaider.actions.git

import com.intellij.openapi.project.Project
import com.intellij.ui.TextFieldWithAutoCompletion
import git4idea.GitUtil
import git4idea.repo.GitRepository
import javax.swing.JComponent

class GitRefComboBox(project: Project) {
    private val repository: GitRepository? = GitUtil.getRepositoryManager(project).repositories.firstOrNull()
    private var currentMode: RefType = RefType.BRANCH
    
    enum class RefType {
        BRANCH, ANY_REF
    }

    private val completionField = TextFieldWithAutoCompletion(
        project,
        BranchCompletionProvider(repository),
        false,
        ""
    )

    fun getComponent(): JComponent = completionField

    fun getText(): String = completionField.text

    fun setText(text: String) {
        completionField.text = text
    }

    fun setMode(mode: RefType) {
        currentMode = mode
        updateCompletions()
    }

    private fun updateCompletions() {
        completionField.setVariants(getRefList())
    }

    private fun getRefList(): List<String> {
        return when (currentMode) {
            RefType.BRANCH -> repository?.branches?.localBranches?.map { it.name } ?: emptyList()
            RefType.ANY_REF -> {
                val tags = repository?.tags?.map { it.name } ?: emptyList()
                val branches = repository?.branches?.localBranches?.map { it.name } ?: emptyList()
                tags + branches
            }
        }
    }

    private class BranchCompletionProvider(private val repository: GitRepository?) :
        TextFieldWithAutoCompletion.StringsCompletionProvider(
            emptyList(),  // Start empty, we'll update variants dynamically
            null
        ) {
        override fun getItems(prefix: String?): MutableCollection<String> {
            return (repository?.branches?.localBranches?.map { it.name } ?: emptyList()) +
                   (repository?.tags?.map { it.name } ?: emptyList())
                   .toMutableList()
        }
    }
}
