package de.andrena.codingaider.providers.processmanagers

import com.intellij.openapi.diagnostic.Logger
import de.andrena.codingaider.providers.AIProcessManager
import de.andrena.codingaider.providers.AIProvider
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Process manager for Claude Code provider
 * Claude Code might have different interaction patterns than Aider
 */
class ClaudeCodeProcessManager : AIProcessManager {
    override val provider: AIProvider = AIProvider.CLAUDE_CODE
    
    private val logger = Logger.getInstance(ClaudeCodeProcessManager::class.java)
    
    private data class ProcessInfo(
        var process: Process? = null,
        var writer: BufferedWriter? = null,
        val isRunning: AtomicBoolean = AtomicBoolean(false),
        var outputFlux: Flux<String>? = null
    )
    
    private val defaultProcess = ProcessInfo()
    private val planProcesses = ConcurrentHashMap<String, ProcessInfo>()
    private val processLock = Any()
    
    override fun startProcess(
        command: List<String>,
        workingDir: String,
        verbose: Boolean,
        planId: String?
    ): Mono<Void> {
        synchronized(processLock) {
            val processInfo = planId?.let {
                planProcesses.getOrPut(it) { ProcessInfo() }
            } ?: defaultProcess
            
            if (processInfo.isRunning.get()) {
                logger.info("Claude Code process already running for ${planId ?: "default"}")
                return Mono.empty()
            }
            
            return Mono.defer {
                val processBuilder = ProcessBuilder(command)
                    .apply {
                        environment().putIfAbsent("PYTHONIOENCODING", "utf-8")
                        directory(File(workingDir))
                        redirectErrorStream(true)
                    }
                
                processInfo.process = processBuilder.start()
                setupProcessStreams(processInfo, planId)
                
                if (verbose) {
                    logger.info("Started Claude Code process with command: ${command.joinToString(" ")}")
                    logger.info("Working directory: $workingDir")
                }
                
                waitForProcessReady(processInfo)
                    .doOnSuccess { processInfo.isRunning.set(true) }
                    .then()
            }
                .timeout(Duration.ofSeconds(30))
                .subscribeOn(Schedulers.boundedElastic())
        }
    }
    
    override fun sendCommandAsync(command: String, planId: String?): Flux<String> {
        val processInfo = synchronized(processLock) {
            planId?.let { planProcesses[it] } ?: defaultProcess
        }
        
        if (!processInfo.isRunning.get() || processInfo.process?.isAlive != true) {
            return Flux.error(IllegalStateException("Claude Code process is not running"))
        }
        
        return Flux.defer {
            try {
                // Claude Code might use different command format
                val formattedCommand = formatCommandForClaudeCode(command)
                processInfo.writer?.write(formattedCommand)
                processInfo.writer?.newLine()
                processInfo.writer?.flush()
                
                logger.info("Sent command to Claude Code: $command")
                
                // Return the output flux filtered for this command's response
                processInfo.outputFlux ?: Flux.empty()
            } catch (e: Exception) {
                logger.error("Error sending command to Claude Code process", e)
                Flux.error(e)
            }
        }
    }
    
    override fun isReadyForCommand(planId: String?): Boolean {
        synchronized(processLock) {
            val processInfo = planId?.let { planProcesses[it] } ?: defaultProcess
            return processInfo.isRunning.get() && processInfo.process?.isAlive == true
        }
    }
    
    override fun interruptCurrentCommand(planId: String?) {
        synchronized(processLock) {
            val processInfo = planId?.let { planProcesses[it] } ?: defaultProcess
            if (processInfo.process?.isAlive == true) {
                try {
                    // Claude Code might handle interruption differently
                    // This is a placeholder - actual implementation depends on Claude Code's capabilities
                    processInfo.writer?.write("interrupt\n")
                    processInfo.writer?.flush()
                    logger.info("Sent interrupt signal to Claude Code process")
                } catch (e: Exception) {
                    logger.error("Error interrupting Claude Code process", e)
                    processInfo.process?.destroyForcibly()
                }
            }
        }
    }
    
    override fun stopProcess(planId: String?) {
        synchronized(processLock) {
            val processInfo = planId?.let { planProcesses[it] } ?: defaultProcess
            disposeProcess(processInfo)
            if (planId != null) {
                planProcesses.remove(planId)
            }
        }
    }
    
    override fun getDisplayName(): String = "Claude Code Process Manager"
    
    override fun dispose() {
        synchronized(processLock) {
            try {
                // Dispose all plan processes
                planProcesses.values.forEach { disposeProcess(it) }
                planProcesses.clear()
                
                // Dispose default process
                disposeProcess(defaultProcess)
                logger.info("Disposed all Claude Code processes")
            } catch (e: Exception) {
                logger.error("Error disposing Claude Code processes", e)
            }
        }
    }
    
    private fun setupProcessStreams(processInfo: ProcessInfo, planId: String?) {
        val inputStream = processInfo.process!!.inputStream
        val outputStream = processInfo.process!!.outputStream
        
        processInfo.writer = BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8))
        
        // Create output flux for reading process responses
        processInfo.outputFlux = Flux.create<String> { sink ->
            val readerThread = Thread {
                try {
                    val buffer = ByteArray(1024)
                    val sb = StringBuilder()
                    
                    while (!Thread.currentThread().isInterrupted && processInfo.process?.isAlive == true) {
                        val bytesRead = inputStream.read(buffer)
                        if (bytesRead > 0) {
                            val chunk = String(buffer, 0, bytesRead, Charsets.UTF_8)
                            sb.append(chunk)
                            
                            // Process complete lines
                            var newlineIndex: Int
                            while (sb.indexOf('\n').also { newlineIndex = it } != -1) {
                                val line = sb.substring(0, newlineIndex).trim()
                                sb.delete(0, newlineIndex + 1)
                                
                                if (line.isNotEmpty()) {
                                    sink.next(line)
                                }
                            }
                        } else if (bytesRead == -1) {
                            break
                        }
                    }
                    
                    // Process any remaining content
                    if (sb.isNotEmpty()) {
                        sink.next(sb.toString().trim())
                    }
                    
                    sink.complete()
                } catch (e: Exception) {
                    if (!Thread.currentThread().isInterrupted) {
                        logger.error("Error reading from Claude Code process", e)
                        sink.error(e)
                    }
                }
            }
            
            readerThread.name = "ClaudeCode-Reader-${planId ?: "default"}"
            readerThread.start()
            
            sink.onDispose {
                readerThread.interrupt()
            }
        }.share()
    }
    
    private fun waitForProcessReady(processInfo: ProcessInfo): Mono<Void> {
        // Claude Code might have different ready indicators than Aider
        // This is a placeholder implementation
        return processInfo.outputFlux!!
            .filter { line -> isProcessReadyIndicator(line) }
            .next()
            .then()
            .timeout(Duration.ofSeconds(30))
    }
    
    private fun isProcessReadyIndicator(line: String): Boolean {
        // Claude Code might output different ready indicators
        // This is a placeholder - actual implementation would depend on Claude Code's output
        return line.contains("Ready") || line.contains("Initialized") || line.trim() == ">"
    }
    
    private fun formatCommandForClaudeCode(command: String): String {
        // Claude Code might require different command formatting
        // This is a placeholder implementation
        return command
    }
    
    private fun disposeProcess(processInfo: ProcessInfo) {
        try {
            processInfo.isRunning.set(false)
            processInfo.writer?.close()
            processInfo.process?.destroyForcibly()
            processInfo.process?.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
        } catch (e: Exception) {
            logger.error("Error disposing Claude Code process", e)
        }
    }
}