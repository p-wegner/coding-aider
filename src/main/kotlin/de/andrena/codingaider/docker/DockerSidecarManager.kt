package de.andrena.codingaider.docker

import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class DockerSidecarManager {
    private val logger = Logger.getInstance(DockerSidecarManager::class.java)


    val containerName = "aider-sidecar"

    fun stopDockerContainer() {
        try {
            // For sidecar mode, stop and remove the named container
            ProcessBuilder("docker", "stop", containerName)
                .redirectErrorStream(true)
                .start()
                .waitFor()
            
            ProcessBuilder("docker", "rm", containerName)
                .redirectErrorStream(true)
                .start()
                .waitFor()
        } catch (e: Exception) {
            logger.warn("Failed to stop Docker container", e)
        } finally {
        }
    }


    fun isContainerRunning(): Boolean {
        return try {
            val process = ProcessBuilder("docker", "ps", "-q", "-f", "name=$containerName")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            output.isNotEmpty()
        } catch (e: Exception) {
            logger.warn("Failed to check Docker container status", e)
            false
        }
    }
}
