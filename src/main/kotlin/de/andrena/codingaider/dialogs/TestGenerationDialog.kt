package de.andrena.codingaider.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.settings.AiderProjectSettings
import de.andrena.codingaider.settings.TestTypeConfiguration
import de.andrena.codingaider.utils.FileTraversal
import javax.swing.JComponent

class TestGenerationDialog(
    private val project: Project,
    private val selectedFiles: Array<VirtualFile>
) : DialogWrapper(project) {
    private val settings = AiderProjectSettings.getInstance(project)
    private val testTypeComboBox = ComboBox<TestTypeConfiguration>()
    private val promptArea = JBTextArea().apply {
        rows = 5
        lineWrap = true
        wrapStyleWord = true
    }

    init {
        title = "Generate Tests"
        init()
        updateTestTypes()
    }

    private fun updateTestTypes() {
        testTypeComboBox.removeAllItems()
        settings.getTestTypes()
            .filter { it.isEnabled }
            .forEach { testTypeComboBox.addItem(it) }
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("Test Type:") {
                cell(testTypeComboBox)
            }
            row("Additional Instructions:") {
                cell(JBScrollPane(promptArea))
                    .resizableColumn()
                    .rows(2)
            }
        }
    }

    override fun doValidate(): ValidationInfo? {
        val testType = getSelectedTestType()
        return when {
            testType == null -> ValidationInfo("Please select a test type")
            settings.getTestTypes().isEmpty() -> ValidationInfo("No test types configured. Please configure test types in Project Settings.")
            else -> null
        }
    }

    override fun doOKAction() {
        val testType = getSelectedTestType() ?: return
        val allFiles = FileTraversal.traverseFilesOrDirectories(selectedFiles)
        
        // Create test file paths based on source files
        val sourceFiles = allFiles.filter { file ->
            !file.filePath.matches(Regex(testType.referenceFilePattern))
        }
        
        val testFilePaths = sourceFiles.map { sourceFile ->
            val fileName = java.io.File(sourceFile.filePath).nameWithoutExtension
            val testFileName = testType.testFilePattern.replace("*", fileName)
            val sourcePath = java.io.File(sourceFile.filePath).parent
            "$sourcePath/$testFileName"
        }
        
        val commandData = CommandData(
            message = buildPrompt(testType, allFiles),
            useYesFlag = true,
            files = allFiles + testFilePaths.map { FileData(it, false) },
            projectPath = project.basePath ?: ""
        )
        
        super.doOKAction()
        IDEBasedExecutor(project, commandData).execute()
    }

    private fun buildPrompt(testType: TestTypeConfiguration, files: List<FileData>): String {
        val (sourceFiles, referenceFiles) = files.partition { file ->
            !file.filePath.matches(Regex(testType.referenceFilePattern))
        }
        
        val sourceFileNames = sourceFiles.map { it.filePath }
        val referenceFileNames = referenceFiles.map { it.filePath }
        
        return """
            Generate tests for the following files: $sourceFileNames
            Test type: ${testType.name}
            
            Reference files to use as examples: $referenceFileNames
            Test files will be generated using pattern: ${testType.testFilePattern}
            
            Use the following template:
            ${testType.promptTemplate}
            
            Additional instructions:
            ${getAdditionalPrompt()}
        """.trimIndent()
    }

    private fun getSelectedTestType(): TestTypeConfiguration? = testTypeComboBox.selectedItem as? TestTypeConfiguration
    private fun getAdditionalPrompt(): String = promptArea.text
}
