package de.andrena.codingaider.utils

import java.io.File
import java.util.regex.Pattern

class FilePathConverter {
    companion object {
        // Pre-compiled patterns for better performance
        private val COMBINED_PATH_PATTERN = Pattern.compile(
            """(?:[A-Za-z]:\\[^<>:"|?*\n\r]+)|(?:/[^<>:"|?*\n\r]+)+|(?:\.{0,2}[/\\][^<>:"|?*\n\r]+)|(?:src\\[^<>:"|?*\n\r]+)"""
        )
        
        private const val CHUNK_SIZE = 8192
        
        private val commonExtensions = setOf(
            "md", "txt", "java", "kt", "py", "js", "html", "css", "xml", "json", 
            "yaml", "yml", "properties", "gradle", "kts", "bat", "sh", "pdf"
        )

        fun convertPathsToMarkdownLinks(text: String, basePath: String? = null): String {
            if (text.length < 100) {  // For very short texts, process directly
                return processChunk(text, basePath)
            }

            // Process larger texts in chunks
            val result = StringBuilder(text.length + (text.length / 10))  // Estimate capacity
            var lastEnd = 0
            
            // Find safe chunk boundaries (at newlines) and process chunks
            while (lastEnd < text.length) {
                var nextEnd = minOf(lastEnd + CHUNK_SIZE, text.length)
                
                // Find the next newline if we're not at the end
                if (nextEnd < text.length) {
                    val newlinePos = text.indexOf('\n', nextEnd)
                    nextEnd = if (newlinePos != -1) newlinePos + 1 else text.length
                }
                
                val chunk = text.substring(lastEnd, nextEnd)
                result.append(processChunk(chunk, basePath))
                lastEnd = nextEnd
            }
            
            return result.toString()
        }

        private fun processChunk(chunk: String, basePath: String?): String {
            val matcher = COMBINED_PATH_PATTERN.matcher(chunk)
            if (!matcher.find()) return chunk

            val result = StringBuilder(chunk.length + 20)
            var lastEnd = 0

            do {
                val start = matcher.start()
                val end = matcher.end()
                val path = matcher.group()

                result.append(chunk, lastEnd, start)

                val processedPath = when {
                    path.startsWith("src\\") -> {
                        val normalized = path.replace("\\", "/")
                        convertPathToLink(normalized, isRelative = true)
                    }
                    path.matches("""[A-Za-z]:\\.*""".toRegex()) -> {
                        convertPathToLink(path)
                    }
                    path.startsWith("/") -> {
                        convertPathToLink(path)
                    }
                    basePath != null -> {
                        val normalized = path.replace("\\", "/")
                        val absolutePath = File(basePath, normalized).absolutePath
                        if (isLikelyValidPath(absolutePath)) {
                            convertPathToLink(normalized, isRelative = true)
                        } else {
                            normalized
                        }
                    }
                    else -> path
                }

                result.append(processedPath)
                lastEnd = end
            } while (matcher.find())

            result.append(chunk, lastEnd, chunk.length)
            return result.toString()
        }

        private fun convertPathToLink(path: String, isRelative: Boolean = false): String {
            if (isLikelyValidPath(path)) {
                val urlPath = if (isRelative) path else "file:${path.replace("\\", "/")}"
                return "[$path]($urlPath)"
            }
            return path
        }

        private fun isLikelyValidPath(path: String): Boolean {
            val file = File(path)
            if (file.exists()) return true
            
            val extension = file.extension.lowercase()
            return extension in commonExtensions
        }
    }
}
