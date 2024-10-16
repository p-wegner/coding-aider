package de.andrena.codingaider.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.inputdialog.AiderContextView

class FilenameCompletionContributor(private val contextView: AiderContextView) : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    resultSet: CompletionResultSet
                ) {
                    val filenames = contextView.getAllFiles().map { FileData(it.filePath).fileName }
                    filenames.forEach { filename ->
                        resultSet.addElement(LookupElementBuilder.create(filename))
                    }
                }
            }
        )
    }
}
