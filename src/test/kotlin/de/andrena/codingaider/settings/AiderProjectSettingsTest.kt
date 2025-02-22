package de.andrena.codingaider.settings

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

class AiderProjectSettingsTest {

    @Test
    fun `test type configuration should maintain properties`() {
        val config = AiderProjectSettings.TestTypeConfiguration(
            name = "Unit Test",
            promptTemplate = "Generate unit test",
            referenceFilePattern = ".*Test\\.kt$",
            testFilePattern = "*Test.kt",
            isEnabled = true
        )

        assertThat(config.name).isEqualTo("Unit Test")
        assertThat(config.promptTemplate).isEqualTo("Generate unit test")
        assertThat(config.referenceFilePattern).isEqualTo(".*Test\\.kt$")
        assertThat(config.testFilePattern).isEqualTo("*Test.kt")
        assertThat(config.isEnabled).isTrue()
    }

    @Test
    fun `project settings should persist test types`() {
        val settings = AiderProjectSettings(null)
        val config = AiderProjectSettings.TestTypeConfiguration(
            name = "Integration Test",
            promptTemplate = "Generate integration test",
            referenceFilePattern = ".*IT\\.kt$",
            testFilePattern = "*IT.kt",
            isEnabled = true
        )

        settings.addTestType(config)
        
        val savedTypes = settings.getTestTypes()
        assertThat(savedTypes).hasSize(1)
        assertThat(savedTypes[0]).usingRecursiveComparison().isEqualTo(config)
    }

    @Test
    fun `project settings should update test types`() {
        val settings = AiderProjectSettings(null)
        val config = AiderProjectSettings.TestTypeConfiguration(
            name = "Original",
            promptTemplate = "Original template",
            referenceFilePattern = ".*Test\\.kt$",
            testFilePattern = "*Test.kt",
            isEnabled = true
        )

        settings.addTestType(config)

        val updatedConfig = config.copy(
            name = "Updated",
            promptTemplate = "Updated template"
        )
        settings.updateTestType(0, updatedConfig)

        val savedTypes = settings.getTestTypes()
        assertThat(savedTypes).hasSize(1)
        assertThat(savedTypes[0]).usingRecursiveComparison().isEqualTo(updatedConfig)
    }

    @Test
    fun `project settings should remove test types`() {
        val settings = AiderProjectSettings(null)
        val config = AiderProjectSettings.TestTypeConfiguration(
            name = "To Remove",
            promptTemplate = "Template",
            referenceFilePattern = ".*Test\\.kt$",
            testFilePattern = "*Test.kt",
            isEnabled = true
        )

        settings.addTestType(config)
        assertThat(settings.getTestTypes()).hasSize(1)

        settings.removeTestType(0)
        assertThat(settings.getTestTypes()).isEmpty()
    }
}
