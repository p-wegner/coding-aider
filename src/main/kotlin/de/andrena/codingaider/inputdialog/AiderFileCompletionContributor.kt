package de.andrena.codingaider.inputdialog

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import de.andrena.codingaider.services.PersistentFileService

class AiderFileCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val project = parameters.editor.project ?: return
                    val fileNames = getFileNamesFromContext(project)

                    fileNames.forEach { fileName ->
                        result.addElement(LookupElementBuilder.create(fileName))
                    }
                }
            }
        )
    }

    private fun getFileNamesFromContext(project: Project): List<String> {
        val persistentFileService = PersistentFileService.getInstance(project)
        return persistentFileService.getPersistentFiles().map { it.filePath.substringAfterLast('/') }
    }
}
