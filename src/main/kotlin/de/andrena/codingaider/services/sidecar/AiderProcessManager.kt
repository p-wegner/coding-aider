package de.andrena.codingaider.services.sidecar

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import io.ktor.util.collections.ConcurrentMap
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
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
        var outputParser: AiderOutputParser? = null,
        var outputFlux: Flux<String>? = null
    )

    private val defaultProcess = ProcessInfo()
    private val planProcesses = ConcurrentMap<String, ProcessInfo>()
    private val processLock = Any()

    private var verbose: Boolean = false

    fun startProcess(
        command: List<String>,
        workingDir: String,
        verbose: Boolean = false,
        planId: String? = null
    ): Mono<Void> {
        synchronized(processLock) {
            val processInfo = planId?.let {
                planProcesses.getOrPut(it) { ProcessInfo() }
            } ?: defaultProcess
            this.verbose = verbose

            if (processInfo.isRunning.get()) {
                logger.info("Aider sidecar process already running for ${planId ?: "default"}")
                return Mono.empty()
            }
            return Mono.defer {
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
                processInfo.writer?.write("\n")
                processInfo.writer?.flush()
                waitForFirstPrompt(processInfo)
                    .doOnSuccess {
                    processInfo.isRunning.set(true)
                }.then()
            }
                .timeout(Duration.ofSeconds(100))
                .subscribeOn(Schedulers.boundedElastic())

        }
    }

    private fun waitForFirstPrompt(processInfo: ProcessInfo): Mono<Void> {
        val promptRegex = Regex("^>\\s*$")

        return processInfo.outputFlux!!
            // Filter out all lines that are not the prompt
            .filter { line -> promptRegex.matches(line.trim()) }
            // Take just the first match
            .next()
            // Next returns a Mono<String>, but we want a Mono<Void>
            // to indicate “done waiting,” so map or then
            .then()
            // Optionally set a timeout, in case the process never produces the prompt
            .timeout(Duration.ofSeconds(10))
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
        private const val MAX_CONCURRENT_PROCESSES = 20
    }

    private fun setupProcessStreams(processInfo: ProcessInfo) {
        processInfo.reader = BufferedReader(InputStreamReader(processInfo.process!!.inputStream))
        processInfo.writer = BufferedWriter(OutputStreamWriter(processInfo.process!!.outputStream))
        processInfo.outputFlux = Flux.create<String> { sink ->
            val readerThread = Thread {
                try {
                    processInfo.reader?.use<BufferedReader, Unit> { reader ->
                        while (true) {
                            val line = reader.readLine() ?: break
                            sink.next(line)
                        }
                    }
                    sink.complete() // End of stream
                } catch (e: Exception) {
                    sink.error(e)
                }
            }
            readerThread.isDaemon = true
            readerThread.start()
            sink.onCancel { readerThread.interrupt() }
        }
            .publish()
            .autoConnect(1)
            .subscribeOn(Schedulers.boundedElastic())
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


    private val promptRegex = Regex("^>\\s*$")

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
        val writeCommandMono = Mono.fromCallable {
            processInfo.writer?.write("$command\n")
            processInfo.writer?.flush()
            true // just a dummy
        }
        val responseFlux = processInfo.outputFlux!!
            // Optionally: skip lines that might be from a previous command if needed
            // .skipUntil { ...some logic to detect the command started... }
            .takeUntil { line -> promptRegex.matches(line) }
            .filter { line -> !promptRegex.matches(line) }

        // 3. Return the flux that:
        //    - first writes the command (Mono),
        //    - then collects lines from the shared flux until the next prompt
        return writeCommandMono.thenMany(responseFlux)
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

