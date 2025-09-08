package de.andrena.codingaider.actions.ide

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
import java.nio.file.Files
import java.nio.file.Paths

class SymlinkToScratchAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Creating symlinks to scratch", true) {
            override fun run(indicator: ProgressIndicator) {
                val symlinkedFiles = mutableListOf<String>()
                val failedFiles = mutableListOf<String>()
                
                indicator.text = "Creating symlinks to scratch folder..."
                indicator.isIndeterminate = false
                
                runBlocking {
                    files.forEachIndexed { index, file ->
                        indicator.fraction = index.toDouble() / files.size
                        indicator.text2 = "Processing ${file.name}"
                        
                        try {
                            if (file.isDirectory) {
                                // For directories, symlink all files recursively
                                symlinkDirectoryToScratch(file, "", symlinkedFiles, failedFiles)
                            } else {
                                // For individual files
                                symlinkFileToScratch(project, file, symlinkedFiles, failedFiles)
                            }
                        } catch (e: Exception) {
                            failedFiles.add("${file.name}: ${e.message}")
                        }
                    }
                }
                
                // Show notification with results
                showNotification(project, symlinkedFiles, failedFiles)
            }
        })
    }
    
    private suspend fun symlinkFileToScratch(project: Project, file: VirtualFile, symlinkedFiles: MutableList<String>, failedFiles: MutableList<String>) {
        try {
            val fileType = FileTypeManager.getInstance().getFileTypeByFile(file)
            val language = Language.findLanguageByID(fileType.name) ?: Language.findLanguageByID("TEXT")
            
            writeAction {
                val scratchFile = ScratchRootType.getInstance().createScratchFile(
                    project,
                    file.name,
                    language,
                    "" // Empty content, we'll create a symlink instead
                )
                
                if (scratchFile != null) {
                    // Create symlink from scratch file to original file
                    createSymlink(scratchFile.path, file.path, symlinkedFiles, failedFiles, file.name)
                } else {
                    failedFiles.add("${file.name}: Failed to create scratch file")
                }
            }
        } catch (e: Exception) {
            failedFiles.add("${file.name}: ${e.message}")
        }
    }
    
    private suspend fun symlinkDirectoryToScratch(directory: VirtualFile, relativePath: String, symlinkedFiles: MutableList<String>, failedFiles: MutableList<String>) {
        directory.children.forEach { child ->
            val childRelativePath = if (relativePath.isEmpty()) child.name else "$relativePath/${child.name}"
            
            if (child.isDirectory) {
                symlinkDirectoryToScratch(child, childRelativePath, symlinkedFiles, failedFiles)
            } else {
                try {
                    val fileType = FileTypeManager.getInstance().getFileTypeByFile(child)
                    val language = Language.findLanguageByID(fileType.name) ?: Language.findLanguageByID("TEXT")
                    
                    // Create unique name for nested files
                    val scratchFileName = if (relativePath.isEmpty()) child.name else "${relativePath.replace('/', '_')}_${child.name}"
                    
                    writeAction {
                        val scratchFile = ScratchRootType.getInstance().createScratchFile(
                            null, // Use null for project to make it available across all projects
                            scratchFileName,
                            language,
                            "" // Empty content, we'll create a symlink instead
                        )
                        
                        if (scratchFile != null) {
                            // Create symlink from scratch file to original file
                            createSymlink(scratchFile.path, child.path, symlinkedFiles, failedFiles, childRelativePath)
                        } else {
                            failedFiles.add("$childRelativePath: Failed to create scratch file")
                        }
                    }
                } catch (e: Exception) {
                    failedFiles.add("$childRelativePath: ${e.message}")
                }
            }
        }
    }
    
    private fun createSymlink(scratchFilePath: String, originalFilePath: String, symlinkedFiles: MutableList<String>, failedFiles: MutableList<String>, fileName: String) {
        try {
            val scratchPath = Paths.get(scratchFilePath)
            val originalPath = Paths.get(originalFilePath)
            
            // Delete the empty scratch file first
            Files.deleteIfExists(scratchPath)
            
            // Create the symbolic link
            Files.createSymbolicLink(scratchPath, originalPath)
            symlinkedFiles.add(fileName)
        } catch (e: Exception) {
            failedFiles.add("$fileName: Failed to create symlink - ${e.message}")
        }
    }
    
    private fun showNotification(project: Project, symlinkedFiles: List<String>, failedFiles: List<String>) {
        val message = buildString {
            if (symlinkedFiles.isNotEmpty()) {
                appendLine("Successfully created ${symlinkedFiles.size} symlink(s) to scratch:")
                symlinkedFiles.take(5).forEach { appendLine("  • $it") }
                if (symlinkedFiles.size > 5) {
                    appendLine("  ... and ${symlinkedFiles.size - 5} more")
                }
            }
            
            if (failedFiles.isNotEmpty()) {
                if (symlinkedFiles.isNotEmpty()) appendLine()
                appendLine("Failed to create ${failedFiles.size} symlink(s):")
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