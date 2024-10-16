package de.andrena.codingaider.inputdialog

import com.intellij.openapi.project.Project
import com.intellij.ui.TextFieldWithAutoCompletionListProvider
import de.andrena.codingaider.command.FileData

class AiderCompletionProvider(project: Project, files: List<FileData>) :
    TextFieldWithAutoCompletionListProvider<String>(
        files.map { it.filePath.substringAfterLast('/') }.distinct()
    ) {
    override fun getLookupString(item: String): String = item

}