package de.andrena.codingaider.executors.api

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory
import java.io.StringReader
import org.xml.sax.InputSource

/**
 * Data class representing a file change in the Aider summary
 */
data class AiderFileChange(
    val path: String,
    val action: String,
    val description: String
)

/**
 * Data class representing the complete Aider summary
 */
data class AiderSummary(
    val status: String,
    val changes: List<AiderFileChange>,
    val error: String? = null
)

/**
 * Parser for Aider's XML summary output
 */
class AiderSummaryParser {
    /**
     * Parse XML summary string into an AiderSummary object
     */
    fun parse(xmlString: String): AiderSummary? {
        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(InputSource(StringReader(xmlString)))
            
            return parseSummaryDocument(document)
        } catch (e: Exception) {
            // Return null if parsing fails
            return null
        }
    }

    private fun parseSummaryDocument(document: Document): AiderSummary {
        val root = document.documentElement
        
        val status = root.getElementsByTagName("status").item(0)?.textContent ?: "unknown"
        val error = root.getElementsByTagName("error").item(0)?.textContent
        
        val changes = parseFileChanges(root.getElementsByTagName("file"))
        
        return AiderSummary(status, changes, error)
    }

    private fun parseFileChanges(fileNodes: NodeList): List<AiderFileChange> {
        return (0 until fileNodes.length).map { i ->
            val fileElement = fileNodes.item(i) as Element
            AiderFileChange(
                path = fileElement.getElementsByTagName("path").item(0)?.textContent ?: "",
                action = fileElement.getElementsByTagName("action").item(0)?.textContent ?: "",
                description = fileElement.getElementsByTagName("description").item(0)?.textContent ?: ""
            )
        }
    }
}
