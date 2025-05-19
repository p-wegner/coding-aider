package de.andrena.codingaider.features.documentation

import java.io.File

data class DocumentTypeConfiguration(
    var name: String = "",
    var promptTemplate: String = "",
    var filePattern: String = "*.md",
    var isEnabled: Boolean = true,
    var contextFiles: List<String> = listOf()
) {
    /**
     * Converts absolute paths to paths relative to the project root
     */
    fun withRelativePaths(projectRoot: String): DocumentTypeConfiguration {
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
    fun withAbsolutePaths(projectRoot: String): DocumentTypeConfiguration {
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
