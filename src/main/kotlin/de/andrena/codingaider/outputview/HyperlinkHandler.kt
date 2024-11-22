package de.andrena.codingaider.outputview

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
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
                
                when {
                    // Handle HTTP(S) URLs
                    url.matches(Regex("^https?://.*")) -> {
                        Desktop.getDesktop().browse(URI(url))
                    }
                    
                    // Handle absolute file paths
                    url.startsWith("file:") -> {
                        openFileInIde(java.net.URLDecoder.decode(url.removePrefix("file:"), "UTF-8"), project)
                    }
                    
                    File(url).isAbsolute -> {
                        openFileInIde(url, project)
                    }
                    
                    // Handle relative paths
                    else -> {
                        val basePath = project?.basePath ?: throw IllegalArgumentException("Project base path not found")
                        val relativePath = url.removePrefix("./")
                        
                        // Try direct path first
                        val directFile = File(basePath, relativePath)
                        if (directFile.exists()) {
                            openFileInIde(directFile.absolutePath, project)
                            return
                        }
                        
                        // Try lookup paths
                        for (lookupPath in lookupPaths) {
                            val lookupFile = File(basePath, "$lookupPath/$relativePath")
                            if (lookupFile.exists()) {
                                openFileInIde(lookupFile.absolutePath, project)
                                return
                            }
                        }
                        
                        // If no file found, try as URL
                        try {
                            Desktop.getDesktop().browse(URI(url))
                        } catch (e: Exception) {
                            throw IllegalArgumentException("Unable to open: $url")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
    private fun openFileInIde(filePath: String, project: Project?) {
        if (project == null) throw IllegalArgumentException("No active project found")
        
        val file = File(filePath)
        if (!file.exists()) throw IllegalArgumentException("File not found: $filePath")
        
        val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file)
            ?: throw IllegalArgumentException("Cannot find virtual file for: $filePath")
            
        OpenFileDescriptor(project, virtualFile).navigate(true)
    }
