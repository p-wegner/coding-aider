package de.andrena.codingaider.utils

import java.io.File

object EnvFileReader {
    fun readEnvFile(file: File): Map<String, String> {
        val envVars = mutableMapOf<String, String>()
        if (file.exists()) {
            file.readLines().forEach { line ->
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim().removeSurrounding("\"", "'")
                    envVars[key] = value
                }
            }
        }
        return envVars
    }
}
