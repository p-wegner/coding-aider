package de.andrena.codingaider.actions.ide

import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.lang.Language
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.runBlocking
import java.io.IOException

class CopyToScratchAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Copying Files to Scratch", true) {
            override fun run(indicator: ProgressIndicator) {
                val copiedFiles = mutableListOf<String>()
                val failedFiles = mutableListOf<String>()
                
                indicator.text = "Copying files to scratch folder..."
                indicator.isIndeterminate = false
                
                runBlocking {
                    files.forEachIndexed { index, file ->
                        indicator.fraction = index.toDouble() / files.size
                        indicator.text2 = "Processing ${file.name}"
                        
                        try {
                            if (file.isDirectory) {
                                // For directories, copy all files recursively
                                copyDirectoryToScratch(file, "", copiedFiles, failedFiles)
                            } else {
                                // For individual files
                                copyFileToScratch(project, file, copiedFiles, failedFiles)
                            }
                        } catch (e: Exception) {
                            failedFiles.add("${file.name}: ${e.message}")
                        }
                    }
                }
                
                // Show notification with results
                showNotification(project, copiedFiles, failedFiles)
            }
        })
    }
    
    private suspend fun copyFileToScratch(project: Project, file: VirtualFile, copiedFiles: MutableList<String>, failedFiles: MutableList<String>) {
        try {
            val content = String(file.contentsToByteArray(), file.charset)
            val fileType = FileTypeManager.getInstance().getFileTypeByFile(file)
            val language = Language.findLanguageByID(fileType.name) ?: Language.findLanguageByID("TEXT")
            
            writeAction {
                val scratchFile = ScratchRootType.getInstance().createScratchFile(
                    project,
                    file.name,
                    language,
                    content
                )
                
                if (scratchFile != null) {
                    copiedFiles.add(file.name)
                } else {
                    failedFiles.add("${file.name}: Failed to create scratch file")
                }
            }
        } catch (e: IOException) {
            failedFiles.add("${file.name}: ${e.message}")
        }
    }
    
    private suspend fun copyDirectoryToScratch(directory: VirtualFile, relativePath: String, copiedFiles: MutableList<String>, failedFiles: MutableList<String>) {
        directory.children.forEach { child ->
            val childRelativePath = if (relativePath.isEmpty()) child.name else "$relativePath/${child.name}"
            
            if (child.isDirectory) {
                copyDirectoryToScratch(child, childRelativePath, copiedFiles, failedFiles)
            } else {
                try {
                    val content = String(child.contentsToByteArray(), child.charset)
                    val fileType = FileTypeManager.getInstance().getFileTypeByFile(child)
                    val language = Language.findLanguageByID(fileType.name) ?: Language.findLanguageByID("TEXT")
                    
                    // Create unique name for nested files
                    val scratchFileName = if (relativePath.isEmpty()) child.name else "${relativePath.replace('/', '_')}_${child.name}"
                    
                    writeAction {
                        val scratchFile = ScratchRootType.getInstance().createScratchFile(
                            null, // Use null for project to make it available across all projects
                            scratchFileName,
                            language,
                            content
                        )
                        
                        if (scratchFile != null) {
                            copiedFiles.add(childRelativePath)
                        } else {
                            failedFiles.add("$childRelativePath: Failed to create scratch file")
                        }
                    }
                } catch (e: IOException) {
                    failedFiles.add("$childRelativePath: ${e.message}")
                }
            }
        }
    }
    
    private fun showNotification(project: Project, copiedFiles: List<String>, failedFiles: List<String>) {
        val message = buildString {
            if (copiedFiles.isNotEmpty()) {
                appendLine("Successfully copied ${copiedFiles.size} file(s) to scratch:")
                copiedFiles.take(5).forEach { appendLine("  • $it") }
                if (copiedFiles.size > 5) {
                    appendLine("  ... and ${copiedFiles.size - 5} more")
                }
            }
            
            if (failedFiles.isNotEmpty()) {
                if (copiedFiles.isNotEmpty()) appendLine()
                appendLine("Failed to copy ${failedFiles.size} file(s):")
                failedFiles.take(3).forEach { appendLine("  • $it") }
                if (failedFiles.size > 3) {
                    appendLine("  ... and ${failedFiles.size - 3} more")
                }
            }
        }
        
        val notificationType = if (failedFiles.isEmpty()) NotificationType.INFORMATION else NotificationType.WARNING
        
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Coding Aider Notifications")
            .createNotification(message.trim(), notificationType)
            .notify(project)
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabledAndVisible = project != null && !files.isNullOrEmpty()
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}