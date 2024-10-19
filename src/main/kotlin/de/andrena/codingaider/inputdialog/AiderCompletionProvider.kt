package de.andrena.codingaider.inputdialog

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNamedElement
import com.intellij.ui.TextFieldWithAutoCompletionListProvider
import de.andrena.codingaider.command.FileData

class AiderCompletionProvider(
    project: Project,
    files: List<FileData>
) : TextFieldWithAutoCompletionListProvider<String>(null) {

    private val classMethodMap: Map<String, List<String>>

    init {
        classMethodMap = extractCompletions(project, files)
        setItems(classMethodMap.keys + classMethodMap.values.flatten())
    }

    override fun getItems(prefix: String, cached: Boolean, parameters: CompletionParameters?): Collection<String> {
        val dotIndex = prefix.lastIndexOf('.')
        return if (dotIndex == -1) {
            // Suggest classes
            classMethodMap.keys.filter { it.startsWith(prefix, ignoreCase = true) }
        } else {
            // Suggest methods for the given class
            val className = prefix.substring(0, dotIndex)
            val methodPrefix = prefix.substring(dotIndex + 1)
            classMethodMap[className]?.filter { it.startsWith(methodPrefix, ignoreCase = true) }
                ?.map { "$className.$it" }
                ?: emptyList()
        }
    }

    override fun getLookupString(item: String): String = item

    private fun extractCompletions(project: Project, files: List<FileData>): Map<String, List<String>> {
        val psiManager = PsiManager.getInstance(project)
        val fileSystem = LocalFileSystem.getInstance()

        return files.flatMap { fileData ->
            val virtualFile = fileSystem.findFileByPath(fileData.filePath) ?: return@flatMap emptyList()
            val psiFile = psiManager.findFile(virtualFile) ?: return@flatMap emptyList()

            extractFromPsiFile(psiFile)
        }.toMap()
    }

    private fun extractFromPsiFile(psiFile: PsiFile): List<Pair<String, List<String>>> {
        val classLikeElements = findClassLikeElements(psiFile)

        return if (classLikeElements.isNotEmpty()) {
            classLikeElements.mapNotNull { classElement ->
                val className = (classElement as? PsiNamedElement)?.name ?: return@mapNotNull null
                val methods = findMethodLikeElements(classElement).mapNotNull { (it as? PsiNamedElement)?.name }
                className to methods
            }
        } else {
            // Fallback: treat the file as a single class
            val fileName = psiFile.name.substringBeforeLast('.')
            val methods = findMethodLikeElements(psiFile).mapNotNull { (it as? PsiNamedElement)?.name }
            listOf(fileName to methods)
        }
    }

    private fun findClassLikeElements(element: PsiElement): List<PsiElement> {
        return element.children.filter {
            it is PsiNamedElement && it.node.elementType.toString().contains("CLASS")
        }
    }

    private fun findMethodLikeElements(element: PsiElement): List<PsiElement> {
        return element.children.filter {
            it is PsiNamedElement && (
                    it.node.elementType.toString().contains("METHOD") ||
                            it.node.elementType.toString().contains("FUNCTION")
                    )
        }
    }
}