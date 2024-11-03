package de.andrena.codingaider.inputdialog

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.elementType
import com.intellij.ui.TextFieldWithAutoCompletionListProvider
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.services.FileExtractorService

class AiderCompletionProvider(
    project: Project,
    files: List<FileData>
) : TextFieldWithAutoCompletionListProvider<String>(null) {

    private val classMethodMap: Map<String, List<String>>

    init {
        val fileExtractorService = FileExtractorService.getInstance()
        val extractedFiles = fileExtractorService.extractFilesIfNeeded(files)
        classMethodMap = extractCompletions(project, extractedFiles)
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
        val topLevelElements = psiFile.children.filterIsInstance<PsiNamedElement>()

        return if (topLevelElements.isNotEmpty()) {
            topLevelElements.mapNotNull { element ->
                when {
                    isClassLike(element) -> extractClassInfo(element)
                    isFunctionLike(element) -> {
                        val fileName = psiFile.name.substringBeforeLast('.')
                        fileName to listOf(element.name ?: return@mapNotNull null)
                    }

                    else -> null
                }
            }
        } else {
            // Fallback: treat the file as a single class
            val fileName = psiFile.name.substringBeforeLast('.')
            val methods = psiFile.findMethodLikeElements().mapNotNull { (it as? PsiNamedElement)?.name }
            listOf(fileName to methods)
        }
    }

    private fun isClassLike(element: PsiElement): Boolean {
        val type = element.node.elementType.toString().uppercase()
        return type.contains("CLASS") || type.contains("OBJECT") || type.contains("INTERFACE")
    }

    private fun isFunctionLike(element: PsiElement): Boolean {
        val type = element.node.elementType.toString().uppercase()
        return type.contains("METHOD") || type.contains("FUNCTION") || type.contains("FUN") || type.contains("SCRIPT")
    }

    private fun isFieldLike(element: PsiElement): Boolean {
        val type = element.node.elementType.toString().uppercase()
        return type.contains("FIELD") || type.contains("PROPERTY")
    }

    private fun extractClassInfo(classElement: PsiElement): Pair<String, List<String>>? {
        val className = (classElement as? PsiNamedElement)?.name ?: return null
        val methods = classElement.findMethodLikeElements().union(classElement.findFieldLikeElements())
            .mapNotNull { (it as? PsiNamedElement)?.name }
        return className to methods
    }

    private fun PsiElement.findMethodLikeElements(): List<PsiElement> =
        classChildren(this).filter { isFunctionLike(it) }

    private fun PsiElement.findFieldLikeElements(): List<PsiElement> =
        classChildren(this).filter { isFieldLike(it) }

    private fun classChildren(element: PsiElement) = element.children.flatMap {
        if (it.elementType.toString().uppercase().contains("CLASS_BODY")) {
            it.children.toList()
        } else {
            listOf(it)
        }
    }
}
