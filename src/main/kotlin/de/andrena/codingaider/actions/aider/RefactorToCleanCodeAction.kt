package de.andrena.codingaider.actions.aider

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.IDEBasedExecutor
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.FileTraversal

class RefactorToCleanCodeAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        refactorToCleanCode(project, files)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabledAndVisible = project != null && !files.isNullOrEmpty()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    companion object {
        private fun refactorToCleanCode(project: Project, virtualFiles: Array<VirtualFile>) {
            val allFiles = FileTraversal.traverseFilesOrDirectories(virtualFiles)
            val fileNames = allFiles.map { it.filePath }

            val settings = AiderSettings.getInstance(project)
            val commandData = CommandData(
                message = buildRefactorInstructions(fileNames),
                useYesFlag = true,
                llm = settings.llm,
                additionalArgs = settings.additionalArgs,
                files = allFiles,
                isShellMode = false,
                lintCmd = settings.lintCmd,
                deactivateRepoMap = settings.deactivateRepoMap,
                editFormat = settings.editFormat,
                projectPath = project.basePath ?: ""
            )
            IDEBasedExecutor(project, commandData).execute()
        }

        private fun buildRefactorInstructions(fileNames: List<String>): String {
            return """
                Analyze and refactor the following files to implement clean code principles and adhere to SOLID principles: $fileNames.
                
                Please follow these steps:
                1. Review the code for adherence to clean code principles:
                   - Meaningful names for variables, functions, and classes
                   - Functions that do one thing
                   - Proper comments and documentation
                   - DRY (Don't Repeat Yourself) principle
                   - Proper formatting and organization
                
                2. Analyze the code structure for adherence to SOLID principles:
                   - Single Responsibility Principle
                   - Open-Closed Principle
                   - Liskov Substitution Principle
                   - Interface Segregation Principle
                   - Dependency Inversion Principle
                
                3. Refactor the code to improve its adherence to these principles:
                   - Break down large functions or classes
                   - Extract reusable code into separate functions or classes
                   - Use appropriate design patterns where applicable
                   - Improve naming conventions
                   - Add or update comments and documentation
                
                4. Ensure that the refactored code maintains the original functionality.
                
                5. Provide a detailed explanation of the changes made, including:
                   - Which clean code principles were applied and how
                   - Which SOLID principles were addressed and how
                   - Any design patterns or architectural improvements implemented
                
                Please make these refactorings while preserving the overall structure and functionality of the code. If any changes might affect the behavior of the code, highlight these in your explanation.
            """.trimIndent()
        }
    }
}
