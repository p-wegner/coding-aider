package de.andrena.codingaider.inputdialog

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.ui.TextFieldWithAutoCompletionListProvider
import de.andrena.codingaider.command.FileData

class AiderCompletionProvider(project: Project, files: List<FileData>) :
    TextFieldWithAutoCompletionListProvider<String>(
        extractCompletions(project, files)
    ) {
    override fun getLookupString(item: String): String = item

    companion object {
        private fun extractCompletions(project: Project, files: List<FileData>): List<String> {
            val completions = mutableListOf<String>()
            val psiManager = PsiManager.getInstance(project)

            for (fileData in files) {
                val psiFile: PsiFile? = psiManager.findFile(fileData.filePath)
                if (psiFile != null) {
                    val classes = PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java)
                    for (psiClass in classes) {
                        val className = psiClass.name
                        val methods = psiClass.methods
                        for (method in methods) {
                            val methodName = method.name
                            if (className != null) {
                                completions.add("$className.$methodName")
                            }
                        }
                    }
                }
            }
            return completions.distinct()
        }
    }
}
