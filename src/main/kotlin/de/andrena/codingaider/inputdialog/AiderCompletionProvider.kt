package de.andrena.codingaider.inputdialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
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

            for (fileData in getVirtualFiles(project, files)) {
                val psiFile: PsiFile? = psiManager.findFile(fileData)
                if (psiFile != null) {
                    val classes = getClasses(psiFile)
                    for (psiClass in classes) {
//                        val className = psiClass.name
//                        val methods = psiClass.methods
//                        for (method in methods) {
//                            val methodName = method.name
//                            if (className != null) {
//                                completions.add("$className.$methodName")
//                            }
//                        }
                    }
                }
            }
            return completions.distinct()
        }

        private fun getClasses(psiFile: PsiFile): List<Any> {
            // TODO: implement this method
            return listOf()
        }

        private fun getVirtualFiles(project: Project, files: List<FileData>): List<VirtualFile> {
            // TODO: implement this
        }
    }
}
