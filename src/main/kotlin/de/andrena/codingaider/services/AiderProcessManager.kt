package de.andrena.codingaider.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
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
    private var process: Process? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private val outputSink = Sinks.many().multicast().onBackpressureBuffer<String>()
    private val isRunning = AtomicBoolean(false)
    private val commandPrompt = "> "
    private val startupTimeout = Duration.ofSeconds(60)
    private var verbose: Boolean = false

    fun startProcess(
        command: List<String>,
        workingDir: String,
        verbose: Boolean = false
    ): Boolean {
        synchronized(this) {
            this.verbose = verbose
            if (isRunning.get()) {
                logger.info("Aider sidecar process already running")
                return true
            }

            return try {
                val processBuilder = ProcessBuilder(command)
                    .apply { environment().putIfAbsent("PYTHONIOENCODING", "utf-8") }
                    .directory(java.io.File(workingDir))
                    .redirectErrorStream(true)

                process = processBuilder.start()
                reader = BufferedReader(InputStreamReader(process!!.inputStream))
                writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))

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

    private fun waitForFirstUserPrompt(): Boolean =
        Mono.create { sink ->
            try {
                var line: String?
                reader?.readLine()
                while (reader!!.readLine().also { line = it } != null) {
                    if (verbose) logger.info(line)
                    outputSink.tryEmitNext(line!!)

                    if (line!!.isEmpty()) {
                        isRunning.set(true)
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

    fun sendCommand(command: String,firstCommand: Boolean = false): Mono<String> {
        if (!isRunning.get()) {
            return Mono.error(IllegalStateException("Aider sidecar process not running"))
        }

        return Mono.create<String> { sink ->
            synchronized(this) {
                try {
                    // Send the command
                    writer?.write("$command\n")
                    writer?.flush()

                    // Collect response until we see the prompt
                    val response = StringBuilder()
                    var commandPromptCount = 0
                    var line: String?
                    while (reader?.readLine().also { line = it } != null) {
                        if (verbose) logger.info(line)
                        if (response.isNotEmpty() && !line!!.isPromptLine()) response.append("\n")
                        if (!line!!.isPromptLine()) response.append(line)

                        if (line == commandPrompt) commandPromptCount++
                        if ( firstCommand){
                            if ( commandPromptCount > 1 &&  line!!.isEmpty()) {
                                sink.success(response.toString())
                                return@synchronized
                            }
                        } else {
                            if ( commandPromptCount > 0 &&  line!!.isEmpty()) {
                                sink.success(response.toString())
                                return@synchronized
                            }
                        }
                    }
                    sink.error(IllegalStateException("Process terminated while waiting for response"))
                } catch (e: Exception) {
                    sink.error(e)
                }
            }
        }
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError { e ->
                logger.error("Error sending command to Aider sidecar process", e)
            }
    }

    override fun dispose() {
        synchronized(this) {
            try {
                isRunning.set(false)
                outputSink.tryEmitComplete()
                writer?.close()
                reader?.close()
                process?.destroy()
                logger.info("Disposed Aider sidecar process")

                // Wait for process to terminate
                Mono.fromCallable { process?.isAlive == true }
                    .repeatWhen { it.delayElements(Duration.ofMillis(100)) }
                    .takeUntil { !it }
                    .timeout(Duration.ofSeconds(5))
                    .doFinally {
                        process?.destroyForcibly()
                    }
                    .subscribe()
            } catch (e: Exception) {
                logger.error("Error disposing Aider sidecar process", e)
            }
        }
    }

    fun isReadyForCommand(): Boolean {
        synchronized(this) {
            if (process?.isAlive != true) return false
            if (!isRunning.get()) return false
            return try {
                writer?.let { it.write("\n"); it.flush() }
                true
            } catch (e: Exception) {
                logger.error("Error checking process readiness", e)
                false
            }
        }
    }
    private fun String.isPromptLine() = this == commandPrompt
    fun sendCommandAsync(command: String, firstCommand: Boolean): Flux<String> {
        if (!isRunning.get()) {
            return Flux.error(IllegalStateException("Aider sidecar process not running"))
        }

        return Flux.create { sink ->
            synchronized(this) {
                try {
                    writer?.write("$command\n")
                    writer?.flush()

                    var commandPromptCount = 0
                    var line: String?
                    while (reader?.readLine().also { line = it } != null) {
                        if (verbose) logger.info(line)
                        if (!line!!.isPromptLine()) {
                            sink.next(line)
                        }

                        if (line == commandPrompt) commandPromptCount++
                        if (firstCommand) {
                            if (commandPromptCount > 1 && line!!.isEmpty()) {
                                sink.complete()
                                return@synchronized
                            }
                        } else {
                            if (commandPromptCount > 0 && line!!.isEmpty()) {
                                sink.complete()
                                return@synchronized
                            }
                        }
                    }
                    sink.error(IllegalStateException("Process terminated while waiting for response"))
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
}
