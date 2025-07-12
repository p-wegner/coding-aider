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
        val tags: List<String> = emptyList(),
        val requiresAuth: Boolean = false
    )
    
    data class RepoInfo(
        val estimatedSizeMB: Double?,
        val defaultBranch: String?,
        val isAccessible: Boolean,
        val requiresAuth: Boolean = false,
        val error: String? = null
    )
    
    data class AuthCredentials(
        val username: String,
        val password: String // Can be password or personal access token
    )
    
    fun getRepositoryInfoAsync(
        repoUrl: String,
        credentials: AuthCredentials? = null
    ): CompletableFuture<RepoInfo> {
        val future = CompletableFuture<RepoInfo>()
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Checking Repository Info", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "Fetching repository information..."
                    indicator.isIndeterminate = true
                    
                    val repoInfo = getRepositoryInfo(repoUrl, credentials)
                    future.complete(repoInfo)
                } catch (e: Exception) {
                    future.complete(RepoInfo(null, null, false, error = e.message))
                }
            }
        })
        
        return future
    }
    
    fun cloneRepositoryAsync(
        repoUrl: String,
        targetBranch: String? = null,
        targetCommit: String? = null,
        credentials: AuthCredentials? = null,
        shallowClone: Boolean = true,
        depth: Int = 1
    ): CompletableFuture<CloneResult> {
        val future = CompletableFuture<CloneResult>()
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Cloning Git Repository", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "Cloning repository..."
                    indicator.isIndeterminate = true
                    
                    val tempDir = createTempDirectory()
                    val result = cloneRepository(repoUrl, tempDir, targetBranch, targetCommit, credentials, shallowClone, depth, indicator)
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
        targetCommit: String?,
        credentials: AuthCredentials?,
        shallowClone: Boolean,
        depth: Int,
        indicator: ProgressIndicator
    ): CloneResult {
        try {
            val git = Git.getInstance()
            val handler = GitLineHandler(project, targetDir.parentFile, GitCommand.CLONE)
            
            // Add authentication if provided
            val authenticatedUrl = if (credentials != null && !repoUrl.startsWith("git@")) {
                // For HTTPS URLs, embed credentials
                val urlWithAuth = repoUrl.replace("https://", "https://${credentials.username}:${credentials.password}@")
                urlWithAuth
            } else {
                repoUrl
            }
            
            handler.addParameters(authenticatedUrl)
            handler.addParameters(targetDir.name)
            
            if (targetBranch != null) {
                handler.addParameters("--branch", targetBranch)
            }
            
            if (shallowClone && targetCommit == null) {
                handler.addParameters("--depth", depth.toString())
                handler.addParameters("--single-branch")
            }
            
            // Set environment variables for SSH authentication if needed
            if (credentials != null && repoUrl.startsWith("git@")) {
                // For SSH URLs, we would need to handle SSH key authentication
                // This is more complex and might require additional setup
            }
            
            val result = git.runCommand(handler)
            
            if (result.success()) {
                // If a specific commit was requested, checkout to that commit
                if (targetCommit != null) {
                    indicator.text = "Checking out specific commit..."
                    val checkoutSuccess = checkoutCommit(targetDir, targetCommit)
                    if (!checkoutSuccess) {
                        return CloneResult(false, null, "Failed to checkout commit: $targetCommit")
                    }
                }
                
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
                val errorOutput = result.errorOutputAsJoinedString
                val requiresAuth = errorOutput.contains("Authentication failed") || 
                                 errorOutput.contains("access denied") ||
                                 errorOutput.contains("repository not found") ||
                                 errorOutput.contains("403") ||
                                 errorOutput.contains("401")
                return CloneResult(false, null, errorOutput, requiresAuth = requiresAuth)
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
    
    fun checkoutCommit(repoDir: File, commitHash: String): Boolean {
        return try {
            val git = Git.getInstance()
            val handler = GitLineHandler(project, repoDir, GitCommand.CHECKOUT)
            handler.addParameters(commitHash)
            
            val result = git.runCommand(handler)
            result.success()
        } catch (e: Exception) {
            false
        }
    }
    
    fun checkoutTag(repoDir: File, tag: String): Boolean {
        return try {
            val git = Git.getInstance()
            val handler = GitLineHandler(project, repoDir, GitCommand.CHECKOUT)
            handler.addParameters("tags/$tag")
            
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
                // Add retry mechanism for Windows file locking issues
                var attempts = 0
                val maxAttempts = 3
                while (attempts < maxAttempts && file.exists()) {
                    try {
                        file.deleteRecursively()
                        break
                    } catch (e: Exception) {
                        attempts++
                        if (attempts < maxAttempts) {
                            Thread.sleep(1000) // Wait 1 second before retry
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Log error but don't throw - cleanup is best effort
        }
    }
    
    fun validateClonedRepository(repoPath: String): Boolean {
        return try {
            val repoDir = File(repoPath)
            val gitDir = File(repoDir, ".git")
            repoDir.exists() && gitDir.exists()
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getRepositoryInfo(repoUrl: String, credentials: AuthCredentials?): RepoInfo {
        return try {
            val git = Git.getInstance()
            val tempDir = createTempDirectory()
            
            // Try to do a very shallow clone to get basic info
            val handler = GitLineHandler(project, tempDir.parentFile, GitCommand.CLONE)
            
            val authenticatedUrl = if (credentials != null && !repoUrl.startsWith("git@")) {
                repoUrl.replace("https://", "https://${credentials.username}:${credentials.password}@")
            } else {
                repoUrl
            }
            
            handler.addParameters(authenticatedUrl)
            handler.addParameters(tempDir.name)
            handler.addParameters("--depth", "1")
            handler.addParameters("--bare")
            handler.addParameters("--quiet")
            
            val result = git.runCommand(handler)
            
            if (result.success()) {
                // Get repository size estimation
                val sizeInBytes = calculateDirectorySize(tempDir)
                val estimatedSizeMB = sizeInBytes / (1024.0 * 1024.0)
                
                // Get default branch
                val defaultBranch = getDefaultBranch(tempDir)
                
                // Cleanup temp directory
                tempDir.deleteRecursively()
                
                RepoInfo(
                    estimatedSizeMB = estimatedSizeMB * 10, // Rough estimation for full clone
                    defaultBranch = defaultBranch,
                    isAccessible = true
                )
            } else {
                val errorOutput = result.errorOutputAsJoinedString
                val requiresAuth = errorOutput.contains("Authentication failed") || 
                                 errorOutput.contains("access denied") ||
                                 errorOutput.contains("repository not found") ||
                                 errorOutput.contains("403") ||
                                 errorOutput.contains("401")
                
                RepoInfo(null, null, false, requiresAuth, errorOutput)
            }
        } catch (e: Exception) {
            RepoInfo(null, null, false, error = e.message)
        }
    }
    
    private fun getDefaultBranch(repoDir: File): String? {
        return try {
            val git = Git.getInstance()
            val handler = GitLineHandler(project, repoDir, GitCommand.REV_PARSE)
            handler.addParameters("--abbrev-ref", "HEAD")
            
            val result = git.runCommand(handler)
            if (result.success() && result.output.isNotEmpty()) {
                result.output.first().trim()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun calculateDirectorySize(directory: File): Long {
        return directory.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }
    
    private fun createTempDirectory(): File {
        val tempDir = Files.createTempDirectory("aider-git-clone").toFile()
        tempDir.deleteOnExit()
        return tempDir
    }
}
