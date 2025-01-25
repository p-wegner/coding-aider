package de.andrena.codingaider.toolwindow.workingdirectory

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.settings.AiderProjectSettings
import java.io.File
import javax.swing.JComponent
import javax.swing.JLabel

class WorkingDirectoryPanel(private val project: Project) {
    private val settings = AiderProjectSettings.getInstance(project)
    private var pathLabel: JLabel? = null

    fun getContent(): JComponent {
        return panel {
            row {
                pathLabel = label(getDisplayPath())
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
        return file.isDirectory && 
               file.absolutePath.startsWith(projectPath) && 
               LocalFileSystem.getInstance().findFileByPath(path)?.exists() == true
    }
}
