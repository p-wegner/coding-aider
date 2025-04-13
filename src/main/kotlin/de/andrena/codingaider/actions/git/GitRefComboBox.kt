package de.andrena.codingaider.actions.git

import com.intellij.openapi.project.Project
import com.intellij.ui.TextFieldWithAutoCompletion
import git4idea.GitUtil
import git4idea.repo.GitRepository
import javax.swing.JComponent

class GitRefComboBox(project: Project) {
    private val repository: GitRepository? = GitUtil.getRepositoryManager(project).repositories.firstOrNull()

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

    private fun getRefList(): List<String> {
        val tags = repository?.getTags().emptyOnNull()
        val branches = repository?.getLocalBranches().emptyOnNull()
        return tags + branches
    }

    private class BranchCompletionProvider(private val repository: GitRepository?) :
        TextFieldWithAutoCompletion.StringsCompletionProvider(
            emptyList(),  // Start empty, we'll update variants dynamically
            null
        ) {
        override fun getVariants(): Collection<String> {
            return repository?.let { 
                it.getLocalBranches().emptyOnNull() + it.getTags().emptyOnNull() 
            } ?: emptyList()
        }

        override fun getLookupString(prefix: String): String {
            val variants = getVariants()
            return variants.firstOrNull { it.startsWith(prefix) } ?: prefix
        }
    }

}

fun List<String>?.emptyOnNull(): List<String> = this ?: emptyList()
fun GitRepository.getTags(): List<String> = this.tagHolder.getTags().keys.map { it.name }
fun GitRepository.getLocalBranches(): List<String> = this.branches.localBranches.map { it.name }
