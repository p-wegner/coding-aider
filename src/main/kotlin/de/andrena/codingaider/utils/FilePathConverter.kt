package de.andrena.codingaider.utils

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class FilePathConverter {
    companion object {
        private val windowsPathRegex = """[A-Za-z]:\\[^<>:"|?*\n\r]+""".toRegex()
        private val unixPathRegex = """(/[^<>:"|?*\n\r]+)+""".toRegex()
        private val relativePathRegex = """\.{0,2}[/\\][^<>:"|?*\n\r]+""".toRegex()
        private val backslashPathRegex = """src\\[^<>:"|?*\n\r]+""".toRegex()
        
        private val commonExtensions = setOf(
            "md", "txt", "java", "kt", "py", "js", "html", "css", "xml", "json", 
            "yaml", "yml", "properties", "gradle", "kts", "bat", "sh", "pdf"
        )

        fun convertPathsToMarkdownLinks(text: String, basePath: String? = null): String {
            val lines = text.lines()
            
            val convertedLines = lines.map { line ->
                var processedLine = line
                
                // Process Windows absolute paths
                processedLine = processedLine.replace(windowsPathRegex) { matchResult ->
                    convertPathToLink(matchResult.value)
                }

                // Process Unix absolute paths
                processedLine = processedLine.replace(unixPathRegex) { matchResult ->
                    convertPathToLink(matchResult.value)
                }

                // Process backslash paths
                processedLine = processedLine.replace(backslashPathRegex) { matchResult ->
                    val path = matchResult.value.replace("\\", "/")
                    convertPathToLink(path, isRelative = true)
                }

                // Process relative paths if basePath is provided
                if (basePath != null) {
                    processedLine = processedLine.replace(relativePathRegex) { matchResult ->
                        val relativePath = matchResult.value.replace("\\", "/")
                        val absolutePath = File(basePath, relativePath).absolutePath
                        if (isLikelyValidPath(absolutePath)) {
                            convertPathToLink(relativePath, isRelative = true)
                        } else {
                            relativePath
                        }
                    }
                }

                processedLine
            }

            return convertedLines.joinToString("\n")
        }

        private fun convertPathToLink(path: String, isRelative: Boolean = false): String {
            if (isLikelyValidPath(path)) {
                val displayPath = path
                val urlPath = if (isRelative) {
                    path
                } else {
                    "file:${path.replace("\\", "/")}"
                }
                return "[$displayPath]($urlPath)"
            }
            return path
        }

        private fun isLikelyValidPath(path: String): Boolean {
            val file = File(path)
            if (file.exists()) return true
            
            // Check if it has a common file extension
            val extension = file.extension.lowercase()
            if (extension in commonExtensions) return true
            
            // Additional heuristics can be added here
            return false
        }

    }
}
