package de.andrena.codingaider.docker

import com.intellij.openapi.diagnostic.Logger
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempFile
import java.util.concurrent.TimeUnit

class DockerContainerManager {
    private val logger = Logger.getInstance(DockerContainerManager::class.java)
    private val cidFile: Path = createTempFile(prefix = "aider_container_id_", suffix = ".tmp")
    private var dockerContainerId: String? = null

    fun getCidFilePath(): String = cidFile.toString()

    fun getDockerContainerId(): String? {
        if (dockerContainerId == null) {
            var attempts = 0
            while (attempts < 10) {
                if (Files.exists(cidFile)) {
                    dockerContainerId = Files.readString(cidFile).trim()
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
                // Clean up the cidfile
                Files.deleteIfExists(cidFile)
            } catch (e: Exception) {
                logger.error("Failed to stop Docker container", e)
            }
        }
    }
}
