package de.andrena.codingaider.actions.git

import com.intellij.openapi.project.Project
import com.intellij.ui.TextFieldWithAutoCompletion
import de.andrena.codingaider.actions.git.getLocalBranches
import de.andrena.codingaider.actions.git.getTags
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


    private class BranchCompletionProvider(private val repository: GitRepository?) :
        TextFieldWithAutoCompletion.StringsCompletionProvider(
            repository?.getLocalBranches().emptyOnNull() + repository?.getTags().emptyOnNull(),
            null
        ) {
        fun getVariants() =
            repository?.getLocalBranches().emptyOnNull() + repository?.getTags().emptyOnNull()  // Use the variants we initialized with

        override fun getLookupString(prefix: String): String {
            val variants = getVariants()
            return variants.firstOrNull { it.startsWith(prefix) } ?: prefix
        }
    }

}

fun List<String>?.emptyOnNull(): List<String> = this ?: emptyList()
fun GitRepository.getTags(): List<String> = this.tagHolder.getTags().keys.map { it.name }
fun GitRepository.getLocalBranches(): List<String> = this.branches.localBranches.map { it.name }
