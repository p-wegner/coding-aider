package de.andrena.codingaider.toolwindow.workingdirectory

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
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
            }
            row {
                button("Select Directory") { selectDirectory() }
                button("Clear") { clearDirectory() }
            }
        }
    }

    private fun getDisplayPath(): String {
        return settings.workingDirectory?.let { "Working Directory: $it" } ?: "No working directory set (using project root)"
    }

    private fun selectDirectory() {
        val descriptor = FileChooserDescriptor(false, true, false, false, false, false)
            .withRoots(project.baseDir)
            .withTitle("Select Working Directory")
            .withDescription("Choose a directory to restrict Aider operations")

        FileChooser.chooseFile(descriptor, project, null) { virtualFile ->
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
        val projectPath = project.basePath ?: return false
        
        if (!file.isDirectory) {
            Messages.showErrorDialog(project, "Selected path is not a directory", "Invalid Working Directory")
            return false
        }
        
        val normalizedPath = FileTraversal.normalizedFilePath(file.absolutePath)
        val normalizedProjectPath = FileTraversal.normalizedFilePath(projectPath)
        
        if (!normalizedPath.startsWith(normalizedProjectPath)) {
            Messages.showErrorDialog(
                project,
                "Working directory must be within the project directory",
                "Invalid Working Directory"
            )
            return false
        }
        
        if (LocalFileSystem.getInstance().findFileByPath(path)?.exists() != true) {
            Messages.showErrorDialog(project, "Directory does not exist", "Invalid Working Directory")
            return false
        }
        
        return true
    }
}
