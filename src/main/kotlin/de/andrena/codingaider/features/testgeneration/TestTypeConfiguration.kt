package de.andrena.codingaider.features.testgeneration

import java.io.File

data class TestTypeConfiguration(
    var name: String = "",
    var promptTemplate: String = "",
    var referenceFilePattern: String = "",
    var testFilePattern: String = "*Test.kt",
    var isEnabled: Boolean = true,
    var contextFiles: List<String> = listOf()
) {
    /**
     * Converts absolute paths to paths relative to the project root
     */
    fun withRelativePaths(projectRoot: String): TestTypeConfiguration {
        val root = File(projectRoot)
        val relativeContextFiles = contextFiles.map { path ->
            val file = File(path)
            if (file.isAbsolute) {
                try {
                    root.toPath().relativize(file.toPath()).toString().replace('\\', '/')
                } catch (e: IllegalArgumentException) {
                    // If the path can't be relativized (e.g., different drive on Windows), keep it as is
                    path
                }
            } else {
                path
            }
        }
        return copy(contextFiles = relativeContextFiles)
    }

    /**
     * Converts relative paths to absolute paths based on the project root
     */
    fun withAbsolutePaths(projectRoot: String): TestTypeConfiguration {
        val root = File(projectRoot)
        val absoluteContextFiles = contextFiles.map { path ->
            val file = File(path)
            if (!file.isAbsolute) {
                File(root, path).absolutePath
            } else {
                path
            }
        }
        return copy(contextFiles = absoluteContextFiles)
    }
}
