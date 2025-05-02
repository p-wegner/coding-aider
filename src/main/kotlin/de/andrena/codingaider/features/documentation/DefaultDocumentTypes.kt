package de.andrena.codingaider.features.documentation

object DefaultDocumentTypes {
    fun getDefaultDocumentTypes(): List<DocumentTypeConfiguration> = listOf(
        DocumentTypeConfiguration(
            name = "Technical Documentation",
            promptTemplate = """Generate technical documentation for the provided code following these guidelines:
                |1. Include a high-level overview of the module's purpose and functionality
                |2. Document key classes, methods, and interfaces
                |3. Explain design patterns and architectural decisions
                |4. Include code examples for common use cases
                |5. Document dependencies between components using a Mermaid diagram""".trimMargin(),
            isEnabled = true
        ),
        DocumentTypeConfiguration(
            name = "PRD (Product Requirements Document)",
            promptTemplate = """Generate a product requirements document focusing on:
                |1. Problem statement and user needs
                |2. Feature descriptions and requirements
                |3. User stories and acceptance criteria
                |4. Technical constraints and dependencies
                |5. Implementation considerations""".trimMargin(),
            isEnabled = true
        ),
        DocumentTypeConfiguration(
            name = "User Guide",
            promptTemplate = """Generate a user guide for the provided code:
                |1. Explain the purpose and functionality from a user perspective
                |2. Include step-by-step instructions for common tasks
                |3. Provide examples of typical usage scenarios
                |4. Document configuration options and customization
                |5. Include troubleshooting information for common issues""".trimMargin(),
            isEnabled = true
        )
    )
}
