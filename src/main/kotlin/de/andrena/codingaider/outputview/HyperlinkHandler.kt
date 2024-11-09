package de.andrena.codingaider.outputview

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import java.awt.Desktop
import java.io.File
import java.net.URI
import javax.swing.event.HyperlinkEvent

class HyperlinkHandler(private val lookupPaths: List<String>) {
    fun handleHyperlinkEvent(event: HyperlinkEvent) {
        if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
            try {
                val url = event.url?.toString() ?: event.description
                val project = ProjectManager.getInstance().openProjects.firstOrNull()
                // log url
                println("Opening URL: $url")
                val file = when {
                    url.startsWith("file:") -> {
                        // Handle absolute file paths
                        val filePath = java.net.URLDecoder.decode(url.removePrefix("file:"), "UTF-8")
                        File(filePath)
                    }

                    else -> {
                        val basePath = project?.basePath
                        if (basePath != null) {
                            val relativePath = url.removePrefix("./")
                            val file = File(basePath, relativePath)
                            if (file.exists()) {
                                file
                            } else {
                                lookupPaths.map { lookupPath ->
                                    File(basePath, "$lookupPath/$relativePath")
                                }.firstOrNull { it.exists() }
                            }
                        } else {
                            throw IllegalArgumentException("Project base path not found")
                        }
                    }

                }

                if (file != null && project != null) {
                    // Open file in IDE
                    OpenFileDescriptor(
                        project,
                        LocalFileSystem.getInstance().findFileByIoFile(file)
                            ?: throw IllegalArgumentException("File not found: ${file.path}")
                    ).navigate(true)
                } else {
                    // For external URLs or when file/project not found, use default browser
                    Desktop.getDesktop().browse(URI(url))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
