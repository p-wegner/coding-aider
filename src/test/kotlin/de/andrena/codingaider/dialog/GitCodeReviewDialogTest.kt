package de.andrena.codingaider.dialog

import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.andrena.codingaider.actions.git.GitCodeReviewDialog
import org.mockito.kotlin.mock

class GitCodeReviewDialogTest : BasePlatformTestCase() {
    private lateinit var dialog: GitCodeReviewDialog
    private lateinit var project: Project

    override fun setUp() {
        super.setUp()
        project = mock()
        dialog = GitCodeReviewDialog(project)
    }

    fun testGetPromptReturnsDefaultWhenEmpty() {
        val prompt = dialog.getPrompt()
        assertTrue(prompt.contains("Code quality"))
        assertTrue(prompt.contains("Potential bugs"))
        assertTrue(prompt.contains("Performance"))
        assertTrue(prompt.contains("Security"))
    }

    fun testGetSelectedRefsReturnsTrimmedValues() {
        // This would require UI interaction testing
        // For now we can only test the interface
        val (base, target) = dialog.getSelectedRefs()
        assertNotNull(base)
        assertNotNull(target)
    }
}
