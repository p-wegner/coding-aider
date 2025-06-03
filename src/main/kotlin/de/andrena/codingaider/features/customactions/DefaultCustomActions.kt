package de.andrena.codingaider.features.customactions

object DefaultCustomActions {
    fun getDefaultCustomActions(): List<CustomActionConfiguration> = listOf(
        CustomActionConfiguration(
            name = "Code Review",
            promptTemplate = """Review the provided code for:
                |1. Code quality and best practices
                |2. Potential bugs or issues
                |3. Performance improvements
                |4. Security considerations
                |5. Maintainability and readability
                |
                |Provide specific suggestions for improvement.""".trimMargin(),
            isEnabled = true
        ),
        CustomActionConfiguration(
            name = "Add Documentation",
            promptTemplate = """Add comprehensive documentation to the provided code:
                |1. Add class and method documentation
                |2. Include parameter and return value descriptions
                |3. Add usage examples where appropriate
                |4. Document any complex logic or algorithms
                |5. Follow the project's documentation standards""".trimMargin(),
            isEnabled = true
        ),
        CustomActionConfiguration(
            name = "Refactor Code",
            promptTemplate = """Refactor the provided code to improve:
                |1. Code structure and organization
                |2. Readability and maintainability
                |3. Performance where possible
                |4. Adherence to SOLID principles
                |5. Removal of code duplication
                |
                |Maintain the existing functionality while improving the code quality.""".trimMargin(),
            isEnabled = true
        )
    )
}
