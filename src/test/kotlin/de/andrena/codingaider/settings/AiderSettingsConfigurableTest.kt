package de.andrena.codingaider.settings

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.andrena.codingaider.utils.ApiKeyChecker
import org.mockito.Mockito.*

class AiderSettingsConfigurableTest : BasePlatformTestCase() {

    fun testApiKeyCheckerIntegration() {
        val mockApiKeyChecker = mock(ApiKeyChecker::class.java)
        `when`(mockApiKeyChecker.getAllLlmOptions()).thenReturn(listOf("option1", "option2"))
        `when`(mockApiKeyChecker.isApiKeyAvailableForLlm("option1")).thenReturn(true)
        `when`(mockApiKeyChecker.isApiKeyAvailableForLlm("option2")).thenReturn(false)

        val configurable = AiderSettingsConfigurable(project, mockApiKeyChecker)

        // Add your assertions here
        // For example:
        // val component = configurable.createComponent()
        // assertTrue(component.toString().contains("option1"))
        // assertTrue(component.toString().contains("option2"))
    }
}
