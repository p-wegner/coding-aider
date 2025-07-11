package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

@Service(Service.Level.PROJECT)
class GitRepoCloneService(private val project: Project) {
    
    data class CloneResult(
        val success: Boolean,
        val localPath: String?,
        val error: String? = null,
        val branches: List<String> = emptyList(),
        val tags: List<String> = emptyList()
    )
    
    fun cloneRepositoryAsync(
        repoUrl: String,
        targetBranch: String? = null
    ): CompletableFuture<CloneResult> {
        val future = CompletableFuture<CloneResult>()
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Cloning Git Repository", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "Cloning repository..."
                    indicator.isIndeterminate = true
                    
                    val tempDir = createTempDirectory()
                    val result = cloneRepository(repoUrl, tempDir, targetBranch, indicator)
                    future.complete(result)
                } catch (e: Exception) {
                    future.complete(CloneResult(false, null, e.message))
                }
            }
        })
        
        return future
    }
    
    private fun cloneRepository(
        repoUrl: String,
        targetDir: File,
        targetBranch: String?,
        indicator: ProgressIndicator
    ): CloneResult {
        try {
            val git = Git.getInstance()
            val handler = GitLineHandler(project, targetDir.parentFile, GitCommand.CLONE)
            
            handler.addParameters(repoUrl)
            handler.addParameters(targetDir.name)
            
            if (targetBranch != null) {
                handler.addParameters("--branch", targetBranch)
            }
            
            val result = git.runCommand(handler)
            
            if (result.success()) {
                indicator.text = "Fetching branches and tags..."
                val branches = getBranches(targetDir)
                val tags = getTags(targetDir)
                
                return CloneResult(
                    success = true,
                    localPath = targetDir.absolutePath,
                    branches = branches,
                    tags = tags
                )
            } else {
                return CloneResult(false, null, result.errorOutputAsJoinedString)
            }
        } catch (e: Exception) {
            return CloneResult(false, null, e.message)
        }
    }
    
    private fun getBranches(repoDir: File): List<String> {
        return try {
            val git = Git.getInstance()
            val handler = GitLineHandler(project, repoDir, GitCommand.BRANCH)
            handler.addParameters("-r")
            
            val result = git.runCommand(handler)
            if (result.success()) {
                result.output
                    .map { it.trim().removePrefix("origin/") }
                    .filter { it.isNotEmpty() && !it.startsWith("HEAD") }
                    .distinct()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun getTags(repoDir: File): List<String> {
        return try {
            val git = Git.getInstance()
            val handler = GitLineHandler(project, repoDir, GitCommand.TAG)
            
            val result = git.runCommand(handler)
            if (result.success()) {
                result.output.filter { it.isNotEmpty() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun switchToBranch(repoDir: File, branch: String): Boolean {
        return try {
            val git = Git.getInstance()
            val handler = GitLineHandler(project, repoDir, GitCommand.CHECKOUT)
            handler.addParameters(branch)
            
            val result = git.runCommand(handler)
            result.success()
        } catch (e: Exception) {
            false
        }
    }
    
    fun getRepositoryRoot(repoPath: String): VirtualFile? {
        val file = File(repoPath)
        return if (file.exists()) {
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
        } else {
            null
        }
    }
    
    fun cleanupRepository(repoPath: String) {
        try {
            val file = File(repoPath)
            if (file.exists()) {
                file.deleteRecursively()
            }
        } catch (e: Exception) {
            // Log error but don't throw - cleanup is best effort
        }
    }
    
    private fun createTempDirectory(): File {
        val tempDir = Files.createTempDirectory("aider-git-clone").toFile()
        tempDir.deleteOnExit()
        return tempDir
    }
}
