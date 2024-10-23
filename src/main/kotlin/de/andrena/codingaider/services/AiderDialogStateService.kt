package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.FileData

@Service(Level.PROJECT)
class AiderDialogStateService(private val project: Project) {
    private var lastState: DialogState? = null

    fun saveState(
        message: String,
        useYesFlag: Boolean,
        llm: String,
        additionalArgs: String,
        files: List<FileData>,
        isShellMode: Boolean,
        isStructuredMode: Boolean
    ) {
        lastState = DialogState(
            message,
            useYesFlag,
            llm,
            additionalArgs,
            files,
            isShellMode,
            isStructuredMode
        )
    }

    fun getLastState(): DialogState? = lastState

    data class DialogState(
        val message: String,
        val useYesFlag: Boolean,
        val llm: String,
        val additionalArgs: String,
        val files: List<FileData>,
        val isShellMode: Boolean,
        val isStructuredMode: Boolean
    )

    companion object {
        @JvmStatic
        fun getInstance(project: Project): AiderDialogStateService =
            project.getService(AiderDialogStateService::class.java)
    }
}
