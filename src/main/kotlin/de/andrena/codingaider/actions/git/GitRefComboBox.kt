package de.andrena.codingaider.actions.git

import com.intellij.openapi.project.Project
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.components.JBTextField
import git4idea.GitUtil
import git4idea.repo.GitRepository
import javax.swing.JComponent

class GitRefComboBox(project: Project) {
    private val repository: GitRepository? = GitUtil.getRepositoryManager(project).repositories.firstOrNull()
    private var currentMode: RefType = RefType.BRANCH
    
    enum class RefType {
        BRANCH, TAG
    }

    private val completionField = TextFieldWithAutoCompletion(
        project,
        CompletionProvider(repository),
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
            RefType.TAG -> repository?.tags?.map { it.name } ?: emptyList()
        }
    }

    private class CompletionProvider(private val repository: GitRepository?) : 
        TextFieldWithAutoCompletion.StringsCompletionProvider(emptyList(), null) {
        
        override fun getItems(prefix: String?, cached: Boolean, 
                            pattern: TextFieldWithAutoCompletion.CompletionPattern): Collection<String> {
            return repository?.branches?.localBranches?.map { it.name } ?: emptyList()
        }
    }
}
