package de.andrena.codingaider.features.customactions

object DefaultCustomActions {
    fun getDefaultCustomActions(): List<CustomActionConfiguration> = listOf(
        CustomActionConfiguration(
            name = "Code Review",
            promptTemplate = """Please review the provided code and provide feedback on:
                |1. Code quality and best practices
                |2. Potential bugs or issues
                |3. Performance considerations
                |4. Maintainability and readability
                |5. Security concerns if applicable""".trimMargin(),
            isEnabled = true
        ),
        CustomActionConfiguration(
            name = "Add Documentation",
            promptTemplate = """Add comprehensive documentation to the provided code:
                |1. Add JavaDoc/KDoc comments for classes and methods
                |2. Include parameter descriptions and return value documentation
                |3. Add inline comments for complex logic
                |4. Ensure documentation follows project conventions
                |5. Include usage examples where appropriate""".trimMargin(),
            isEnabled = true
        ),
        CustomActionConfiguration(
            name = "Refactor Code",
            promptTemplate = """Refactor the provided code to improve:
                |1. Code structure and organization
                |2. Method and variable naming
                |3. Reduce code duplication
                |4. Apply SOLID principles
                |5. Improve readability and maintainability
                |Keep the same functionality while making the code cleaner.""".trimMargin(),
            isEnabled = true
        )
    )
}
