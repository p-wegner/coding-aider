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
    private val userPromptMarker = "> "

    fun startProcess(
        command: List<String>,
        workingDir: String,
        maxIdleTime: Int = 0,
        autoRestart: Boolean = false,
        verbose: Boolean = false
    ): Boolean {
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

            // Start processing output lines reactively
            Flux.fromStream { reader!!.lines() }
                .publishOn(Schedulers.boundedElastic())
                .doOnNext { line ->
                    outputSink.tryEmitNext(line)
                    println(line)
                    if (line.startsWith(userPromptMarker) || line.trim().isEmpty()) {
                        isRunning.set(true)
                    }
                }
                .subscribe()

            if (verbose) {
                logger.info("Started Aider sidecar process with command: ${command.joinToString(" ")}")
                logger.info("Working directory: $workingDir")
                logger.info("Max idle time: $maxIdleTime")
                logger.info("Auto restart: $autoRestart")
            }

            // Wait for process to be ready by looking for the prompt marker
            val isReady = Mono.fromCallable { isRunning.get() }
                .repeatWhen { it.delayElements(Duration.ofMillis(100)) }
                .takeUntil { it }
                .timeout(Duration.ofSeconds(30))
                .blockOptional()
                .orElse(false)

            if (isReady) {
                logger.info("Aider sidecar process started and ready")
                true
            } else {
                logger.error("Aider sidecar process failed to become ready within timeout")
                dispose()
                false
            }
        } catch (e: Exception) {
            logger.error("Failed to start Aider sidecar process", e)
            false
        }
    }

    fun sendCommand(command: String): Mono<String> {
        if (!isRunning.get()) {
            return Mono.error(IllegalStateException("Aider sidecar process not running"))
        }

        return Mono.fromCallable {
            writer?.write("$command\n")
            writer?.flush()
        }.then(
            outputSink.asFlux()
                .takeUntil { it.startsWith(userPromptMarker) }
                .reduce(StringBuilder()) { sb, line -> sb.append(line).append("\n") }
                .map { it.toString().trim() }
                .doOnError { e ->
                    logger.error("Error sending command to Aider sidecar process", e)
                }
                .filter { isRunning.get() }
                .switchIfEmpty(Mono.error(IllegalStateException("Aider sidecar process stopped unexpectedly")))
        )
    }

    override fun dispose() {
        try {
            outputSink.tryEmitComplete()
            writer?.close()
            reader?.close()
            process?.destroy()
            isRunning.set(false)
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

    fun isReadyForCommand(): Boolean {
        if (process?.isAlive != true) return false
        return isRunning.get()
    }
}
