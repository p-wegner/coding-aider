package de.andrena.codingaider.outputview

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.intellij.plugins.markdown.ui.preview.MarkdownPreviewFileEditor

class MarkdownPreviewFileEditorUtil {
    companion object {
        // Support api changes in the Markdown plugin
        fun createMarkdownPreviewEditor(project: Project, virtualFile: VirtualFile, document: Document): MarkdownPreviewFileEditor {
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