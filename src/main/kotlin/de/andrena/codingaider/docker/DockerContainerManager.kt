package de.andrena.codingaider.docker

import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

class DockerContainerManager {
    private val logger = Logger.getInstance(DockerContainerManager::class.java)
    private val cidFile = File(System.getProperty("java.io.tmpdir"), "aider_container_id.tmp")
    private var dockerContainerId: String? = null

    fun getCidFilePath(): String = cidFile.absolutePath

    fun getDockerContainerId(): String? {
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
            logger.warn("Failed to read Docker container ID from cidfile")
        }
        return dockerContainerId
    }

    fun stopDockerContainer() {
        dockerContainerId?.let { containerId ->
            try {
                val processBuilder = ProcessBuilder("docker", "kill", containerId)
                val stopProcess = processBuilder.start()
                if (!stopProcess.waitFor(5, TimeUnit.SECONDS)) {
                    stopProcess.destroyForcibly()
                    logger.warn("Docker stop command timed out")
                }
            } catch (e: Exception) {
                logger.error("Failed to stop Docker container", e)
            } finally {
                removeCidFile()
            }
        }
    }

    fun removeCidFile() {
        try {
            if (cidFile.exists()) {
                cidFile.delete()
                logger.info("CID file removed successfully")
            }
        } catch (e: Exception) {
            logger.error("Failed to remove CID file", e)
        }
    }
}
