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
                val processBuilder = ProcessBuilder(command)
                    .apply { environment().putIfAbsent("PYTHONIOENCODING", "utf-8") }
                    .directory(java.io.File(workingDir))
                    .redirectErrorStream(true)

                processInfo.process = processBuilder.start()
                processInfo.reader = BufferedReader(InputStreamReader(processInfo.process!!.inputStream))
                processInfo.writer = BufferedWriter(OutputStreamWriter(processInfo.process!!.outputStream))
                processInfo.outputParser = DefaultAiderOutputParser(verbose, logger, processInfo.reader, processInfo.writer)

                if (verbose) {
                    logger.info("Started Aider sidecar process with command: ${command.joinToString(" ")}")
                    logger.info("Working directory: $workingDir")
                }

                waitForFirstUserPrompt()

            } catch (e: Exception) {
                logger.error("Failed to start Aider sidecar process", e)
                false
            }
        }
    }

    private fun waitForFirstUserPrompt(processInfo: ProcessInfo = defaultProcess): Boolean =
        Mono.create { sink ->
            try {
                var line: String?
                processInfo.reader?.readLine()
                // TODO: make more robust, read until char 62 is encountered and no further characters are read within a timeout
                while (processInfo.reader!!.readLine().also { line = it } != null) {
                    if (verbose) logger.info(line)
                    if (line!!.isEmpty()) {
                        processInfo.isRunning.set(true)
                        sink.success(true)
                        break
                    }
                }
            } catch (e: Exception) {
                sink.error(e)
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

        return Flux.create { sink: FluxSink<String> ->
            synchronized(processLock) {
                val parser = processInfo.outputParser ?: DefaultAiderOutputParser(verbose, logger, processInfo.reader, processInfo.writer)
                parser.writeCommandAndReadResults(command, sink)
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

    fun disposePlanProcess(planId: String) {
        synchronized(processLock) {
            planProcesses[planId]?.let { processInfo ->
                if (processInfo.isRunning.get()) {
                    logger.info("Disposing Aider sidecar process for completed plan: $planId")
                    disposeProcess(processInfo)
                    planProcesses.remove(planId)
                    logger.info("Successfully disposed Aider sidecar process for plan $planId")
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
            if (processInfo.process?.isAlive != true) return false
            if (!processInfo.isRunning.get()) return false
            return true
        }
    }

    fun getPlanProcessIds(): Set<String> {
        synchronized(processLock) {
            return planProcesses.keys.toSet()
        }
    }

}

