package de.andrena.codingaider.services.sidecar

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import de.andrena.codingaider.settings.AiderSettings
import io.ktor.util.collections.ConcurrentMap
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink
import reactor.core.scheduler.Schedulers
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
class AiderProcessManager() : Disposable {
    private val logger = Logger.getInstance(AiderProcessManager::class.java)

    private data class ProcessInfo(
        var process: Process? = null,
        var reader: BufferedReader? = null,
        var writer: BufferedWriter? = null,
        val isRunning: AtomicBoolean = AtomicBoolean(false),
        var outputParser: AiderOutputParser? = null
    )

    private val defaultProcess = ProcessInfo()
    private val planProcesses = ConcurrentMap<String, ProcessInfo>()
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
                        directory(File(workingDir))
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
        val workingDirFile = File(workingDir)
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
                    // TODO: when to cleanup?
//                    cleanupFailedProcess(existingProcess!!)
//                    planProcesses.remove(planId)
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
        val settings = AiderSettings.getInstance()
        processInfo.outputParser = RobustAiderOutputParser(verbose, logger, processInfo.reader, processInfo.writer)
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
            readOutputUntilLongerPause(processInfo, sink)
        }
            .timeout(startupTimeout)
            .doOnError { error ->
                logger.error("Aider sidecar process failed to become ready: ${error.message}")
                dispose()
            }
            .onErrorReturn(false)
            .block() ?: false

    private fun readOutputUntilLongerPause(
        processInfo: ProcessInfo,
        sink: MonoSink<Boolean>
    ) {
        val buffer = StringBuilder()
        val promptPattern = Regex("(?m)^\\s*>\\s*$") // Matches '>' at line start/end with optional whitespace
        val errorPattern = Regex("(?i)error:|exception:|failed|terminated") // Common error indicators

        try {
            // Start async reader thread
            val readerThread = Thread {
                try {
                    val reader = processInfo.reader!!
                    val charBuffer = CharArray(4096)
                    
                    while (processInfo.process?.isAlive == true) {
                        val count = reader.read(charBuffer)
                        if (count == -1) break
                        
                        synchronized(buffer) {
                            buffer.append(charBuffer, 0, count)
                            
                            // Check for startup completion indicators
                            val content = buffer.toString()
                            
                            // Check for errors first
                            if (errorPattern.find(content) != null) {
                                sink.error(IllegalStateException("Error detected during startup: $content"))
                                return@synchronized
                            }
                            
                            // Look for prompt pattern
                            if (promptPattern.find(content) != null) {
                                if (verbose) logger.debug("Detected prompt pattern in output")
                                processInfo.isRunning.set(true)
                                sink.success(true)
                                return@synchronized
                            }
                        }
                        
                        // Small delay to prevent CPU spinning
                        Thread.sleep(10)
                    }
                } catch (e: Exception) {
                    sink.error(e)
                }
            }.apply {
                isDaemon = true
                start()
            }

            // Set timeout for startup
            Thread {
                Thread.sleep(startupTimeout.toMillis())
                if (!processInfo.isRunning.get()) {
                    synchronized(buffer) {
                        val output = buffer.toString()
                        sink.error(IllegalStateException("Startup timeout. Last output: $output"))
                    }
                }
            }.start()

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
                    val parser = processInfo.outputParser ?: DefaultAiderOutputParser(
                        verbose,
                        logger,
                        processInfo.reader,
                        processInfo.writer
                    )
                    parser.writeCommandAndReadResults(command, sink)
                } catch (e: Exception) {
                    sink.error(e)
                }
            }
        }
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError { e ->
                logger.error("Error sending async command to Aider sidecar process", e)
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

    // TODO: use to cleanup when plan completed
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
//            if (processInfo.reader?.ready() == true) {
//                processInfo.reader?.mark(1)
//                val available = processInfo.reader?.read()
//                if (available == -1) {
//                    logger.error("Process streams have been closed")
//                    cleanupFailedProcess(processInfo)
//                    return false
//                }
//                processInfo.reader?.reset()
//            }
            // Verify process can still accept commands with timeout
//            if (!verifyProcessResponsiveness(processInfo)) {
//                logger.error("Process is not responsive to commands")
//                cleanupFailedProcess(processInfo)
//                return false
//            }

            // Check process health and resource usage
            try {
                val exitValue = processInfo.process?.exitValue()
                if (exitValue != null) {
                    logger.error("Process has terminated with exit code: $exitValue")
                    cleanupFailedProcess(processInfo)
                    return false
                }
                return true
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

}

