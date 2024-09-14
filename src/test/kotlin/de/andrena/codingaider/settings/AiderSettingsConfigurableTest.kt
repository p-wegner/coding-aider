package de.andrena.codingaider.settings

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.andrena.codingaider.utils.ApiKeyChecker
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class AiderSettingsConfigurableTest : BasePlatformTestCase() {
    @Test
    @Disabled("write tests after checking https://plugins.jetbrains.com/docs/intellij/testing-plugins.html")
    fun testApiKeyCheckerIntegration() {
        val mockApiKeyChecker = mock(ApiKeyChecker::class.java)
        `when`(mockApiKeyChecker.getAllLlmOptions()).thenReturn(listOf("option1", "option2"))
        `when`(mockApiKeyChecker.isApiKeyAvailableForLlm("option1")).thenReturn(true)
        `when`(mockApiKeyChecker.isApiKeyAvailableForLlm("option2")).thenReturn(false)

        val configurable = AiderSettingsConfigurable(project)

        // Add your assertions here
        // For example:
        // val component = configurable.createComponent()
        // assertTrue(component.toString().contains("option1"))
        // assertTrue(component.toString().contains("option2"))
    }
}
