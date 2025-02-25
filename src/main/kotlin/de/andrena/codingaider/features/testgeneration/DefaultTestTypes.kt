package de.andrena.codingaider.features.testgeneration

object DefaultTestTypes {
    fun getDefaultTestTypes(): List<TestTypeConfiguration> = listOf(
        TestTypeConfiguration(
            name = "Unit Test",
            promptTemplate = """Generate a unit test for the provided code following these guidelines:
                |1. Follow the given/when/then pattern
                |2. Include edge cases and error scenarios
                |3. Try to adhere to the DRY and KISS principles, e.g. by using helper methods
                |4. Use descriptive test method names
                |5. Add comments explaining complex test scenarios""".trimMargin(),
            referenceFilePattern = ".*Test\\.(kt|java|ts|cs|py|go)$",
            testFilePattern = "*Test.{kt,java,ts,cs,py,go}",
            isEnabled = true
        ),
        TestTypeConfiguration(
            name = "Integration Test",
            promptTemplate = """Generate an integration test focusing on component interaction:
                |1. Test the integration between multiple components
                |2. Include setup and teardown of resources
                |3. Test both success and failure scenarios
                |4. Use appropriate mocking where needed
                |5. Consider transaction boundaries""".trimMargin(),
            referenceFilePattern = ".*IT\\.(kt|java|ts|cs|py|go)$",
            testFilePattern = "*IT.{kt,java,ts,cs,py,go}",
            isEnabled = true
        ),
        TestTypeConfiguration(
            name = "BDD Specification",
            promptTemplate = """Generate a BDD-style test specification:
                |1. Use descriptive feature/scenario language
                |2. Follow the given/when/then pattern strictly
                |3. Include scenario outlines for parameterized tests
                |4. Add clear documentation comments
                |5. Focus on business requirements""".trimMargin(),
            referenceFilePattern = ".*Spec\\.(kt|java|ts|cs|py|go)$",
            testFilePattern = "*Spec.{kt,java,ts,cs,py,go}",
            isEnabled = true
        )
    )
}
