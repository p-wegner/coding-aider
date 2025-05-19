package de.andrena.codingaider.features.documentation

object DefaultDocumentTypes {
    fun getDefaultDocumentTypes(): List<DocumentTypeConfiguration> = listOf(
        DocumentTypeConfiguration(
            name = "Technical Documentation",
            promptTemplate = """Generate technical documentation for the provided code following these guidelines:
                |1. Include a high-level overview of the module's purpose and functionality
                |2. Document the architecture and design patterns used
                |3. Describe key classes, methods, and interfaces
                |4. Include code examples where appropriate
                |5. Document dependencies between components
                |6. Use diagrams (mermaid) to illustrate complex relationships""".trimMargin(),
            filePattern = "technical_*.md",
            isEnabled = true
        ),
        DocumentTypeConfiguration(
            name = "Product Requirements Document",
            promptTemplate = """Generate a Product Requirements Document (PRD) based on the provided code following these guidelines:
                |1. Include a clear product overview and objectives
                |2. Document user stories and use cases
                |3. List functional and non-functional requirements
                |4. Describe user interfaces and user flows
                |5. Include acceptance criteria
                |6. Document constraints and assumptions""".trimMargin(),
            filePattern = "prd_*.md",
            isEnabled = true
        ),
        DocumentTypeConfiguration(
            name = "User Guide",
            promptTemplate = """Generate a user guide for the provided code following these guidelines:
                |1. Include a clear introduction and purpose
                |2. Provide step-by-step instructions for common tasks
                |3. Include screenshots or diagrams where appropriate
                |4. Document configuration options and settings
                |5. Include troubleshooting information
                |6. Use a friendly, non-technical tone""".trimMargin(),
            filePattern = "userguide_*.md",
            isEnabled = true
        )
    )
}
