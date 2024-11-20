package de.andrena.codingaider.services

import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class AiderDocsService {
    companion object {
        const val AIDER_DOCS_FOLDER = ".coding-aider-docs"
    }
}