package de.andrena.codingaider.utils

import de.andrena.codingaider.settings.AiderSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CopilotAuthCheckerTest {

    private lateinit var settings: AiderSettings

    @BeforeEach
    fun setUp() {
        settings = mock {
            on { ghExecutablePath } doReturn "gh"
            on { copilotProviderBaseUrl } doReturn ""
        }
    }

    @Test
    fun `returns ByokConfigured when provider base URL is set`() {
        whenever(settings.copilotProviderBaseUrl).thenReturn("https://api.example.com/v1")
        val checker = CopilotAuthChecker(settings)
        assertThat(checker.checkAuth()).isEqualTo(CopilotAuthChecker.AuthStatus.ByokConfigured)
    }

    @Test
    fun `returns ByokConfigured and isAuthAvailable true for BYOK`() {
        whenever(settings.copilotProviderBaseUrl).thenReturn("http://localhost:11434/v1")
        val checker = CopilotAuthChecker(settings)
        assertThat(checker.isAuthAvailable()).isTrue()
    }

    @Test
    fun `returns Authenticated when gh auth status exits with 0`() {
        val checker = CopilotAuthChecker(settings) { _ ->
            CopilotAuthChecker.ProcessRunResult("Logged in to github.com as testuser", 0)
        }
        assertThat(checker.checkAuth()).isEqualTo(CopilotAuthChecker.AuthStatus.Authenticated)
    }

    @Test
    fun `isAuthAvailable returns true when authenticated`() {
        val checker = CopilotAuthChecker(settings) { _ ->
            CopilotAuthChecker.ProcessRunResult("Logged in to github.com as testuser", 0)
        }
        assertThat(checker.isAuthAvailable()).isTrue()
    }

    @Test
    fun `returns NotAuthenticated when gh auth status exits non-zero`() {
        val checker = CopilotAuthChecker(settings) { _ ->
            CopilotAuthChecker.ProcessRunResult("You are not logged into any GitHub hosts.", 1)
        }
        val result = checker.checkAuth()
        assertThat(result).isInstanceOf(CopilotAuthChecker.AuthStatus.NotAuthenticated::class.java)
        assertThat((result as CopilotAuthChecker.AuthStatus.NotAuthenticated).message)
            .contains("Not authenticated with GitHub")
            .contains("gh auth login")
    }

    @Test
    fun `isAuthAvailable returns false when not authenticated`() {
        val checker = CopilotAuthChecker(settings) { _ ->
            CopilotAuthChecker.ProcessRunResult("You are not logged into any GitHub hosts.", 1)
        }
        assertThat(checker.isAuthAvailable()).isFalse()
    }

    @Test
    fun `returns GhNotFound when gh executable is not on PATH`() {
        whenever(settings.ghExecutablePath).thenReturn("gh-nonexistent-binary-xyz")
        val checker = CopilotAuthChecker(settings)
        val result = checker.checkAuth()
        assertThat(result).isInstanceOf(CopilotAuthChecker.AuthStatus.GhNotFound::class.java)
    }

    @Test
    fun `isAuthAvailable returns false when gh not found`() {
        whenever(settings.ghExecutablePath).thenReturn("gh-nonexistent-binary-xyz")
        val checker = CopilotAuthChecker(settings)
        assertThat(checker.isAuthAvailable()).isFalse()
    }

    @Test
    fun `returns GhNotFound when processRunner throws exception`() {
        val checker = CopilotAuthChecker(settings) { _ ->
            throw RuntimeException("binary not found")
        }
        val result = checker.checkAuth()
        assertThat(result).isInstanceOf(CopilotAuthChecker.AuthStatus.GhNotFound::class.java)
        assertThat((result as CopilotAuthChecker.AuthStatus.GhNotFound).message)
            .contains("binary not found")
    }

    @Test
    fun `passes correct gh auth status command to process runner`() {
        var capturedCommand: List<String>? = null
        val checker = CopilotAuthChecker(settings) { command ->
            capturedCommand = command
            CopilotAuthChecker.ProcessRunResult("", 0)
        }
        checker.checkAuth()
        assertThat(capturedCommand).containsExactly("gh", "auth", "status")
    }

    @Test
    fun `uses custom ghExecutablePath in process runner command`() {
        whenever(settings.ghExecutablePath).thenReturn("/usr/local/bin/gh")
        var capturedCommand: List<String>? = null
        val checker = CopilotAuthChecker(settings) { command ->
            capturedCommand = command
            CopilotAuthChecker.ProcessRunResult("", 0)
        }
        checker.checkAuth()
        assertThat(capturedCommand?.first()).isEqualTo("/usr/local/bin/gh")
    }
}
