package de.andrena.codingaider.toolwindow.workingdirectory

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.settings.AiderProjectSettings
import de.andrena.codingaider.utils.FileTraversal
import java.io.File
import javax.swing.JComponent
import javax.swing.JLabel

class WorkingDirectoryPanel(private val project: Project) {
    private val settings = AiderProjectSettings.getInstance(project)
    private var pathLabel: JLabel? = null

    fun getContent(): JComponent {
        return panel {
            row {
                pathLabel = label(getDisplayPath()).component
                    .apply { 
                        toolTipText = "When set, Aider operations will be restricted to files within this directory"
                    }
            }
            row {
                button("Select Directory") { selectDirectory() }
                    .apply {
                        toolTipText = "Choose a working directory to restrict Aider operations"
                    }
                button("Clear") { clearDirectory() }
                    .apply {
                        toolTipText = "Reset to use project root directory"
                    }
            }
        }
    }

    private fun getDisplayPath(): String {
        return settings.workingDirectory?.let { path ->
            val normalizedPath = FileTraversal.normalizedFilePath(path)
            "Working Directory: $normalizedPath (Aider operations restricted to this subtree)"
        } ?: "No working directory set (using project root)"
    }

    private fun selectDirectory() {
        val descriptor = FileChooserDescriptor(false, true, false, false, false, false)
            .withRoots(project.baseDir)
            .withTitle("Select Working Directory")
            .withDescription("Choose a directory to restrict Aider operations")

        val defaultDir = project.basePath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
        FileChooser.chooseFile(descriptor, project, defaultDir) { virtualFile ->
            val path = virtualFile.path
            if (isValidWorkingDirectory(path)) {
                settings.workingDirectory = path
                pathLabel?.text = getDisplayPath()
            }
        }
    }

    private fun clearDirectory() {
        settings.workingDirectory = null
        pathLabel?.text = getDisplayPath()
    }

    private fun isValidWorkingDirectory(path: String): Boolean {
        val file = File(path)
        val projectPath = project.basePath ?: run {
            Messages.showErrorDialog(project, "Project base path not found", "Invalid Working Directory")
            return false
        }
        
        if (!file.exists()) {
            Messages.showErrorDialog(project, "Directory does not exist", "Invalid Working Directory")
            return false
        }

        if (!file.isDirectory) {
            Messages.showErrorDialog(project, "Selected path is not a directory", "Invalid Working Directory")
            return false
        }
        
        val normalizedPath = FileTraversal.normalizedFilePath(file.canonicalPath)
        val normalizedProjectPath = FileTraversal.normalizedFilePath(File(projectPath).canonicalPath)
        
        if (!normalizedPath.startsWith(normalizedProjectPath)) {
            Messages.showErrorDialog(
                project,
                "Working directory must be within the project directory.\nSelected: $normalizedPath\nProject: $normalizedProjectPath",
                "Invalid Working Directory"
            )
            return false
        }

        // Check if directory is readable
        if (!file.canRead()) {
            Messages.showErrorDialog(project, "Directory is not readable", "Invalid Working Directory")
            return false
        }
        
        return true
    }
}
