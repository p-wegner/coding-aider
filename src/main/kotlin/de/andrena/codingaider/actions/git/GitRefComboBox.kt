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
            RefType.ANY_REF ->  emptyList()
        }
    }

    private class BranchCompletionProvider(private val repository: GitRepository?) :
        TextFieldWithAutoCompletion.StringsCompletionProvider(repository?.branches?.localBranches?.map { it.name } ?: emptyList(), null) {
        
    }
}
