package de.andrena.codingaider.utils

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class FilePathConverter {
    companion object {
        private val windowsPathRegex = """[A-Za-z]:\\[^<>:"|?*\n\r]+""".toRegex()
        private val unixPathRegex = """(/[^<>:"|?*\n\r]+)+""".toRegex()

        fun convertPathsToMarkdownLinks(text: String): String {
            var result = text
            
            // Process Windows paths
            result = result.replace(windowsPathRegex) { matchResult ->
                val path = matchResult.value
                if (File(path).exists()) {
                    "[${path}](file:${path.replace("\\", "/")})"
                } else {
                    path
                }
            }

            // Process Unix paths
            result = result.replace(unixPathRegex) { matchResult ->
                val path = matchResult.value
                if (File(path).exists()) {
                    "[${path}](file:${path})"
                } else {
                    path
                }
            }

            return result
        }

        fun isLikelyFilePath(text: String): Boolean {
            return windowsPathRegex.matches(text) || unixPathRegex.matches(text)
        }
    }
}
