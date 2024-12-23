package de.andrena.codingaider.services.sidecar

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
class AiderProcessManager(private val project: Project) : Disposable {
    private val logger = Logger.getInstance(AiderProcessManager::class.java)
    private data class ProcessInfo(
        var process: Process? = null,
        var reader: BufferedReader? = null,
        var writer: BufferedWriter? = null,
        val isRunning: AtomicBoolean = AtomicBoolean(false),
        var outputParser: AiderOutputParser? = null
    )
    
    private val defaultProcess = ProcessInfo()
    private val planProcesses = mutableMapOf<String, ProcessInfo>()
    private val processLock = Any()

    private val startupTimeout = Duration.ofSeconds(60)
    private var verbose: Boolean = false
    fun startProcess(
        command: List<String>,
        workingDir: String,
        verbose: Boolean = false,
        planId: String? = null
    ): Boolean {
        synchronized(processLock) {
            val processInfo = planId?.let { 
                planProcesses.getOrPut(it) { ProcessInfo() }
            } ?: defaultProcess
            this.verbose = verbose
            
            if (processInfo.isRunning.get()) {
                logger.info("Aider sidecar process already running for ${planId ?: "default"}")
                return true
            }

            return try {
                validateStartupConditions(workingDir, planId)
                
                val processBuilder = ProcessBuilder(command)
                    .apply { 
                        environment().putIfAbsent("PYTHONIOENCODING", "utf-8")
                        directory(java.io.File(workingDir))
                        redirectErrorStream(true)
                    }

                processInfo.process = processBuilder.start()
                setupProcessStreams(processInfo)

                if (verbose) {
                    logger.info("Started Aider sidecar process with command: ${command.joinToString(" ")}")
                    logger.info("Working directory: $workingDir")
                }

                val started = waitForFirstUserPrompt(processInfo)
                if (!started) {
                    cleanupFailedProcess(processInfo)
                }
                started

            } catch (e: Exception) {
                val errorMsg = when (e) {
                    is SecurityException -> "Security error starting process: ${e.message}"
                    is IllegalStateException -> e.message
                    else -> "Failed to start Aider sidecar process: ${e.message}"
                }
                logger.error(errorMsg, e)
                cleanupFailedProcess(processInfo)
                false
            }
        }
    }

    private fun validateStartupConditions(workingDir: String, planId: String? = null) {
        val workingDirFile = java.io.File(workingDir)
        if (!workingDirFile.exists()) {
            throw IllegalStateException("Working directory does not exist: $workingDir")
        }
        if (!workingDirFile.isDirectory) {
            throw IllegalStateException("Working directory path is not a directory: $workingDir")
        }
        if (!workingDirFile.canRead() || !workingDirFile.canWrite()) {
            throw IllegalStateException("Insufficient permissions for working directory: $workingDir")
        }
        
        // Additional validation for concurrent processes
        synchronized(processLock) {
            if (planId == null && planProcesses.size >= MAX_CONCURRENT_PROCESSES) {
                throw IllegalStateException("Maximum number of concurrent processes ($MAX_CONCURRENT_PROCESSES) reached")
            }
            if (planId != null && planProcesses.containsKey(planId)) {
                // For plan processes, check if existing process is still valid
                val existingProcess = planProcesses[planId]
                if (existingProcess?.isRunning?.get() == true && existingProcess.process?.isAlive == true) {
                    throw IllegalStateException("Process for plan $planId is already running")
                } else {
                    // Clean up invalid process
                    cleanupFailedProcess(existingProcess!!)
                    planProcesses.remove(planId)
                }
            }
        }
    }

    companion object {
        private const val MAX_CONCURRENT_PROCESSES = 5
    }

    private fun setupProcessStreams(processInfo: ProcessInfo) {
        processInfo.reader = BufferedReader(InputStreamReader(processInfo.process!!.inputStream))
        processInfo.writer = BufferedWriter(OutputStreamWriter(processInfo.process!!.outputStream))
        processInfo.outputParser = if (settings.useSidecarMode) {
            EagerAiderOutputParser(verbose, logger, processInfo.reader, processInfo.writer)
        } else {
            DefaultAiderOutputParser(verbose, logger, processInfo.reader, processInfo.writer)
        }
    }

    private fun cleanupFailedProcess(processInfo: ProcessInfo) {
        try {
            processInfo.isRunning.set(false)
            processInfo.writer?.close()
            processInfo.reader?.close()
            processInfo.process?.destroyForcibly()
        } catch (e: Exception) {
            logger.error("Error cleaning up failed process", e)
        }
    }

    private fun waitForFirstUserPrompt(processInfo: ProcessInfo = defaultProcess): Boolean =
        Mono.create { sink ->
            try {
                var line: String?
                var lastChar: Int? = null
                var lastCharTime = System.currentTimeMillis()
                val charTimeout = Duration.ofMillis(500)

                processInfo.reader?.readLine()
                while (processInfo.reader!!.ready() || processInfo.process?.isAlive == true) {
                    if (processInfo.reader!!.ready()) {
                        val currentChar = processInfo.reader!!.read()
                        if (currentChar == -1) break
                        
                        if (verbose) logger.debug("Read char: ${currentChar.toChar()}")
                        
                        if (currentChar == 62) { // '>' character
                            lastChar = currentChar
                            lastCharTime = System.currentTimeMillis()
                        } else if (lastChar == 62 && 
                            System.currentTimeMillis() - lastCharTime > charTimeout.toMillis()) {
                            processInfo.isRunning.set(true)
                            sink.success(true)
                            return@create
                        }
                    } else {
                        Thread.sleep(50)
                    }
                }
                
                if (!processInfo.process?.isAlive!!) {
                    val exitCode = processInfo.process?.exitValue() ?: -1
                    sink.error(IllegalStateException("Process terminated with exit code $exitCode before becoming ready"))
                } else {
                    sink.error(IllegalStateException("Process stream ended before prompt was detected"))
                }
            } catch (e: Exception) {
                val errorMsg = when (e) {
                    is SecurityException -> "Security error during process startup: ${e.message}"
                    is IllegalStateException -> e.message
                    else -> "Unexpected error during process startup: ${e.message}"
                }
                logger.error(errorMsg, e)
                sink.error(IllegalStateException(errorMsg, e))
            }
        }
        .timeout(startupTimeout)
        .doOnError { error ->
            logger.error("Aider sidecar process failed to become ready: ${error.message}")
            dispose()
        }
        .onErrorReturn(false)
        .block() ?: false



    fun sendCommandAsync(command: String, planId: String? = null): Flux<String> {
        val processInfo = synchronized(processLock) {
            planId?.let { planProcesses[it] } ?: defaultProcess
        }
        
        if (!processInfo.isRunning.get()) {
            return Flux.error(IllegalStateException("Aider sidecar process not running for ${planId ?: "default"}"))
        }

        if (!checkProcessStatus(processInfo)) {
            return Flux.error(IllegalStateException("Aider sidecar process is not responsive"))
        }

        return Flux.create { sink: FluxSink<String> ->
            synchronized(processLock) {
                try {
                    val parser = processInfo.outputParser ?: DefaultAiderOutputParser(verbose, logger, processInfo.reader, processInfo.writer)
                    parser.writeCommandAndReadResults(command, sink)
                } catch (e: Exception) {
                    // If command fails, try to recover process state
                    if (!recoverProcessState(processInfo, planId)) {
                        sink.error(IllegalStateException("Failed to recover process state after error", e))
                    } else {
                        sink.error(e)
                    }
                }
            }
        }
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError { e ->
                logger.error("Error sending async command to Aider sidecar process", e)
            }
    }

    private fun recoverProcessState(processInfo: ProcessInfo, planId: String?): Boolean {
        return try {
            // First try gentle recovery
            if (tryGentleRecovery(processInfo)) {
                logger.info("Successfully recovered process state through gentle recovery")
                return true
            }

            // If gentle recovery fails, try hard reset
            logger.info("Gentle recovery failed, attempting hard reset")
            if (tryHardReset(processInfo, planId)) {
                logger.info("Successfully recovered process state through hard reset")
                return true
            }

            logger.error("All recovery attempts failed")
            false
        } catch (e: Exception) {
            logger.error("Failed to recover process state", e)
            false
        }
    }

    private fun tryGentleRecovery(processInfo: ProcessInfo): Boolean {
        return try {
            // Try to reset process state
            processInfo.writer?.write("/clear\n")
            processInfo.writer?.flush()
            Thread.sleep(500) // Wait for clear command to complete
            
            // Verify process is still responsive
            if (!verifyProcessResponsiveness(processInfo)) {
                logger.error("Process not responsive after gentle recovery attempt")
                return false
            }
            
            true
        } catch (e: Exception) {
            logger.debug("Gentle recovery failed", e)
            false
        }
    }

    private fun tryHardReset(processInfo: ProcessInfo, planId: String?): Boolean {
        return try {
            // Force cleanup of current process
            cleanupFailedProcess(processInfo)
            Thread.sleep(1000) // Wait for cleanup

            // Try to start a new process
            val command = processInfo.process?.info()?.commandLine()?.split(" ") ?: return false
            val workingDir = processInfo.process?.info()?.command()?.parent ?: return false
            
            return startProcess(command, workingDir, verbose, planId)
        } catch (e: Exception) {
            logger.error("Hard reset failed", e)
            false
        }
    }

    fun interruptCurrentCommand(planId: String? = null) {
        synchronized(processLock) {
            val processInfo = planId?.let { planProcesses[it] } ?: defaultProcess
            if (processInfo.process?.isAlive == true) {
                if (System.getProperty("os.name").lowercase().contains("windows")) {
                    // Windows specific handling
                    Runtime.getRuntime().exec("cmd.exe /C taskkill /F /T /PID ${processInfo.process!!.pid()}")
                } else {
                    // Unix-based systems (Linux, macOS)
                    processInfo.process?.descendants()?.forEach { processHandle ->
                        processHandle.destroyForcibly()
                    }
                    processInfo.process?.pid()?.let { pid ->
                        Runtime.getRuntime().exec("kill -SIGINT $pid")
                    }
                }
                logger.info("Sent interrupt signal to Aider process for ${planId ?: "default"}")
            }
        }
    }

    fun disposePlanProcess(planId: String) {
        synchronized(processLock) {
            planProcesses[planId]?.let { processInfo ->
                if (processInfo.isRunning.get()) {
                    logger.info("Disposing Aider sidecar process for completed plan: $planId")
                    try {
                        // Send clear command before disposal
                        processInfo.writer?.write("/clear\n")
                        processInfo.writer?.flush()
                        Thread.sleep(100) // Brief wait for command to complete
                        
                        // Send drop command to ensure clean state
                        processInfo.writer?.write("/drop\n")
                        processInfo.writer?.flush()
                        Thread.sleep(100)
                        
                        disposeProcess(processInfo)
                        planProcesses.remove(planId)
                        logger.info("Successfully disposed Aider sidecar process for plan $planId")
                    } catch (e: Exception) {
                        logger.error("Error during plan process disposal", e)
                        // Force cleanup even if disposal fails
                        processInfo.isRunning.set(false)
                        planProcesses.remove(planId)
                    } finally {
                        // Ensure process is terminated
                        processInfo.process?.destroyForcibly()
                    }
                } else {
                    logger.info("No running process found for plan $planId")
                    planProcesses.remove(planId)
                }
            }
        }
    }

    private fun disposeProcess(processInfo: ProcessInfo) {
        try {
            processInfo.isRunning.set(false)
            processInfo.writer?.close()
            processInfo.reader?.close()
            processInfo.process?.destroy()

            // Wait for process to terminate
            Mono.fromCallable { processInfo.process?.isAlive == true }
                .repeatWhen { it.delayElements(Duration.ofMillis(100)) }
                .takeUntil { !it }
                .timeout(Duration.ofSeconds(5))
                .doFinally {
                    processInfo.process?.destroyForcibly()
                }
                .subscribe()
        } catch (e: Exception) {
            logger.error("Error disposing Aider process", e)
        }
    }

    override fun dispose() {
        synchronized(processLock) {
            try {
                // Dispose all plan processes
                planProcesses.values.forEach { disposeProcess(it) }
                planProcesses.clear()
                
                // Dispose default process
                disposeProcess(defaultProcess)
                logger.info("Disposed all Aider sidecar processes")
            } catch (e: Exception) {
                logger.error("Error disposing Aider sidecar process", e)
            }
        }
    }

    fun isReadyForCommand(planId: String? = null): Boolean {
        synchronized(processLock) {
            val processInfo = planId?.let { planProcesses[it] } ?: defaultProcess
            return checkProcessStatus(processInfo)
        }
    }

    private fun checkProcessStatus(processInfo: ProcessInfo): Boolean {
        if (processInfo.process?.isAlive != true) {
            logger.debug("Process is not alive")
            return false
        }
        
        if (!processInfo.isRunning.get()) {
            logger.debug("Process is not marked as running")
            return false
        }

        try {
            // Check if streams are still valid and process is responsive
            if (processInfo.reader?.ready() == true) {
                processInfo.reader?.mark(1)
                val available = processInfo.reader?.read()
                if (available == -1) {
                    logger.error("Process streams have been closed")
                    cleanupFailedProcess(processInfo)
                    return false
                }
                processInfo.reader?.reset()
            }
            
            // Verify process can still accept commands with timeout
            if (!verifyProcessResponsiveness(processInfo)) {
                logger.error("Process is not responsive to commands")
                if (!tryProcessRecovery(processInfo)) {
                    cleanupFailedProcess(processInfo)
                    return false
                }
            }
            
            // Check process health and resource usage
            try {
                val exitValue = processInfo.process?.exitValue()
                if (exitValue != null) {
                    logger.error("Process has terminated with exit code: $exitValue")
                    cleanupFailedProcess(processInfo)
                    return false
                }
                
                // Check process resource usage
                val processHandle = processInfo.process?.toHandle()
                if (processHandle?.isAlive == true) {
                    val info = processHandle.info()
                    if (info.totalCpuDuration().isPresent && 
                        info.totalCpuDuration().get().seconds > 3600) { // 1 hour CPU time
                        logger.warn("Process has high CPU usage, considering restart")
                        return tryProcessRecovery(processInfo)
                    }
                }
            } catch (e: IllegalThreadStateException) {
                // Process is still running
                return true
            }
            
            return true
        } catch (e: Exception) {
            logger.error("Error checking process status", e)
            cleanupFailedProcess(processInfo)
            return false
        }
    }

    private fun verifyProcessResponsiveness(processInfo: ProcessInfo): Boolean {
        return try {
            // Send a no-op command to verify process responsiveness
            processInfo.writer?.write("\n")
            processInfo.writer?.flush()
            
            // Wait briefly for response
            var attempts = 0
            while (attempts++ < 5) {
                if (processInfo.reader?.ready() == true) {
                    return true
                }
                Thread.sleep(100)
            }
            false
        } catch (e: Exception) {
            logger.error("Error verifying process responsiveness", e)
            false
        }
    }

    fun getPlanProcessIds(): Set<String> {
        synchronized(processLock) {
            return planProcesses.keys.toSet()
        }
    }

}

