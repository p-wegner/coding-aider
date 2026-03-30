package de.andrena.codingaider.utils

import de.andrena.codingaider.settings.AiderSettings

class CopilotAuthChecker(
    private val settings: AiderSettings = AiderSettings.getInstance(),
    private val processRunner: (command: List<String>) -> ProcessRunResult = { command ->
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        ProcessRunResult(process.inputStream.bufferedReader().readText(), process.waitFor())
    }
) {

    data class ProcessRunResult(val output: String, val exitCode: Int)

    sealed class AuthStatus {
        object Authenticated : AuthStatus()
        object ByokConfigured : AuthStatus()
        data class NotAuthenticated(val message: String) : AuthStatus()
        data class GhNotFound(val message: String) : AuthStatus()
    }

    fun checkAuth(): AuthStatus {
        if (settings.copilotProviderBaseUrl.isNotEmpty()) {
            return AuthStatus.ByokConfigured
        }

        val ghPath = settings.ghExecutablePath.ifBlank { "gh" }
        return try {
            val result = processRunner(listOf(ghPath, "auth", "status"))
            if (result.exitCode == 0) {
                AuthStatus.Authenticated
            } else {
                AuthStatus.NotAuthenticated(
                    "Not authenticated with GitHub.\n${result.output}\nRun 'gh auth login' to authenticate."
                )
            }
        } catch (e: Exception) {
            AuthStatus.GhNotFound(
                "Could not find 'gh' CLI at '$ghPath': ${e.message}\n" +
                        "Install the GitHub CLI and ensure it is on your PATH."
            )
        }
    }

    fun isAuthAvailable(): Boolean = when (checkAuth()) {
        is AuthStatus.Authenticated, AuthStatus.ByokConfigured -> true
        else -> false
    }
}
