package de.andrena.codingaider.outputview

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import org.intellij.plugins.markdown.ui.preview.MarkdownPreviewFileEditor
import java.awt.Desktop
import java.net.URI
import javax.swing.JEditorPane
import javax.swing.event.HyperlinkEvent

class MarkdownPreviewFileEditorUtil {
    companion object {
        // Support api changes in the Markdown plugin
        fun createMarkdownPreviewEditor(
            project: Project,
            virtualFile: VirtualFile,
            document: Document
        ): MarkdownPreviewFileEditor {
            val editor = EditorFactory.getInstance().createEditor(document, project, virtualFile, true)
            val editorClass = MarkdownPreviewFileEditor::class.java
            val constructors = editorClass.declaredConstructors
            // log the constructors
            for (constructor in constructors) {
                println("Constructor: ${constructor.parameterTypes.joinToString(", ")}")
            }

            // Try to find constructor with Project, VirtualFile, Editor parameters
            val threeParamConstructor1 = constructors.find { constructor ->
                constructor.parameterTypes.size == 3 &&
                        constructor.parameterTypes[0] == Project::class.java &&
                        constructor.parameterTypes[1] == VirtualFile::class.java &&
                        constructor.parameterTypes[2] == com.intellij.openapi.editor.Editor::class.java
            }

            if (threeParamConstructor1 != null) {
                return threeParamConstructor1.newInstance(project, virtualFile, editor) as MarkdownPreviewFileEditor
            }

            // Try to find constructor with Project, VirtualFile, Document parameters
            val threeParamConstructor2 = constructors.find { constructor ->
                constructor.parameterTypes.size == 3 &&
                        constructor.parameterTypes[0] == Project::class.java &&
                        constructor.parameterTypes[1] == VirtualFile::class.java &&
                        constructor.parameterTypes[2] == com.intellij.openapi.editor.Document::class.java
            }

            if (threeParamConstructor2 != null) {
                return threeParamConstructor2.newInstance(project, virtualFile, document) as MarkdownPreviewFileEditor
            }

            // Fall back to constructor with Project, VirtualFile parameters
            val twoParamConstructor = constructors.find { constructor ->
                constructor.parameterTypes.size == 2 &&
                        constructor.parameterTypes[0] == Project::class.java &&
                        constructor.parameterTypes[1] == VirtualFile::class.java
            } ?: throw IllegalStateException("No suitable constructor found for MarkdownPreviewFileEditor")

            return (twoParamConstructor.newInstance(project, virtualFile) as MarkdownPreviewFileEditor).apply {
                setMainEditor(editor)
            }
        }
    }
}

class CustomMarkdownViewer {
    val component: JEditorPane = JEditorPane().apply {
        contentType = "text/html"
        isEditable = false
        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
    }
    private val options = MutableDataSet()
    private val parser = Parser.builder(options).build()
    private val renderer = HtmlRenderer.builder(options).build()

    init {
        component.addHyperlinkListener { event ->
            if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(URI(event.url.toString()))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun setMarkdownContent(markdown: String) {
        val document = parser.parse(markdown)
        val html = renderer.render(document)
        val styledHtml = """
            <html>
            <head>
                <style>
                    body { font-family: sans-serif; margin: 20px; }
                    pre { background-color: #f5f5f5; padding: 10px; border-radius: 5px; }
                    code { font-family: monospace; }
                    a { color: #2196F3; }
                    img { max-width: 100%; }
                </style>
            </head>
            <body>
                $html
            </body>
            </html>
        """.trimIndent()
        component.text = styledHtml
        component.caretPosition = 0
    }
}

class CustomMarkdownViewer {
    val component: JEditorPane = JEditorPane().apply {
        contentType = "text/html"
        isEditable = false
        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
    }
    private val options = MutableDataSet()
    private val parser = Parser.builder(options).build()
    private val renderer = HtmlRenderer.builder(options).build()

    init {
        component.addHyperlinkListener { event ->
            if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(URI(event.url.toString()))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun setMarkdownContent(markdown: String) {
        val document = parser.parse(markdown)
        val html = renderer.render(document)
        val styledHtml = """
            <html>
            <head>
                <style>
                    body { font-family: sans-serif; margin: 20px; }
                    pre { background-color: #f5f5f5; padding: 10px; border-radius: 5px; }
                    code { font-family: monospace; }
                    a { color: #2196F3; }
                    img { max-width: 100%; }
                </style>
            </head>
            <body>
                $html
            </body>
            </html>
        """.trimIndent()
        component.text = styledHtml
        component.caretPosition = 0
    }
}
