package de.andrena.codingaider.docker

import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class DockerContainerManager {
    private val logger = Logger.getInstance(DockerContainerManager::class.java)
    private val uniqueId = UUID.randomUUID().toString()
    private val cidFile = File(System.getProperty("java.io.tmpdir"), "aider_container_id_$uniqueId.tmp")
    private var dockerContainerId: String? = null

    fun getCidFilePath(): String = cidFile.absolutePath

    private fun getDockerContainerId(): String? {
        if (dockerContainerId == null) {
            var attempts = 0
            while (attempts < 10) {
                if (cidFile.exists()) {
                    dockerContainerId = cidFile.readText().trim()
                    return dockerContainerId
                }
                Thread.sleep(500)
                attempts++
            }
            logger.warn("Failed to read Docker container ID from cidfile: ${cidFile.absolutePath}")
        }
        return dockerContainerId
    }

    fun stopDockerContainer() {
        try {
            // For sidecar mode, stop and remove the named container
            ProcessBuilder("docker", "stop", "aider-sidecar")
                .redirectErrorStream(true)
                .start()
                .waitFor()
            
            ProcessBuilder("docker", "rm", "aider-sidecar")
                .redirectErrorStream(true)
                .start()
                .waitFor()
        } catch (e: Exception) {
            logger.warn("Failed to stop Docker container", e)
        } finally {
            removeCidFile()
        }
    }

    fun removeCidFile() {
        try {
            if (cidFile.exists()) {
                cidFile.delete()
                logger.info("CID file removed successfully: ${cidFile.absolutePath}")
            }
        } catch (e: Exception) {
            logger.error("Failed to remove CID file: ${cidFile.absolutePath}", e)
        }
    }

    fun isContainerRunning(): Boolean {
        return try {
            val process = ProcessBuilder("docker", "ps", "-q", "-f", "name=aider-sidecar")
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
