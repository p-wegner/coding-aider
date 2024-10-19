package de.andrena.codingaider.inputdialog

import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
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

            for (virtualFile in getVirtualFiles(project, files)) {
                val psiFile: PsiFile? = psiManager.findFile(virtualFile)
                if (psiFile != null) {
                    val psi = psiFile.viewProvider.getPsi(Language.findLanguageByID("JAVA")!!)
                    val elements = PsiTreeUtil.collectElements(
                        psi
                    ) {
                        listOf(
                            "CLASS",
                            "METHOD",
                            "FIELD"
                        ).contains(it.parent.elementType.toString()) && it.elementType.toString() == "IDENTIFIER"
                    }
//                    elements.forEach { println(it.text) }
                    completions.addAll(elements.map { toCompletion(it) })
                    //                    val classes = getClasses(psiFile)
//                    for (psiClass in classes) {
//                        val className = psiClass.name
//                        val methods = psiClass.methods
//                        for (method in methods) {
//                            val methodName = method.name
//                            if (className != null) {
//                                completions.add("$className.$methodName")
//                            }
//                        }
//                    }
                }
            }
            return completions.distinct()
        }

        private fun toCompletion(it: PsiElement): String {
            if (it.parent.elementType.toString() == "CLASS") {
                return it.text
            }
            if (it.parent.elementType.toString() == "METHOD") {
                return "${it.parent.text}.${it.text}"
            }
            if (it.parent.elementType.toString() == "FIELD") {
                return "${it.parent.parent.text}.${it.text}"
            }
            return it.text
        }

//        private fun getClasses(psiFile: PsiFile): List<PsiClass> {
//            val classes = mutableListOf<PsiClass>()
//            psiFile.accept(object : PsiRecursiveElementVisitor() {
//                override fun visitElement(element: PsiElement) {
//                    if (element is PsiClass) {
//                        classes.add(element)
//                    }
//                    super.visitElement(element)
//                }
//            })
//            return classes
//        }

        private fun getVirtualFiles(project: Project, files: List<FileData>): List<VirtualFile> {
            val virtualFileManager = VirtualFileManager.getInstance()
            return files.mapNotNull { fileData ->
                virtualFileManager.findFileByUrl("file://${fileData.filePath}")
            }
        }
    }
}
