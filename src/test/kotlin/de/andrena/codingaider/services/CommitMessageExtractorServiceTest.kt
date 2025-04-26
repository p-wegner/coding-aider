package de.andrena.codingaider.services

import com.intellij.openapi.project.Project
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.nio.file.Files
import java.nio.file.Paths

class CommitMessageExtractorServiceTest {

    private lateinit var extractor: CommitMessageExtractorService
    private lateinit var mockProject: Project

    @BeforeEach
    fun setUp() {
        mockProject = mock(Project::class.java)
        extractor = CommitMessageExtractorService(mockProject)
    }

    @Test
    fun `extractCommitMessage should return null for empty input`() {
        val input = ""
        val result = extractor.extractCommitMessage(input)
        assertThat(result).isNull()
    }

    @Test
    fun `extractCommitMessage should return null when no commit message tag is present`() {
        val input = """
            This is a response without any commit message tags.
            It has some code changes but no commit message.
            
            src/main/kotlin/App.kt
            ```kotlin
            fun main() {
                println("Hello, World!")
            }
            ```
        """.trimIndent()
        
        val result = extractor.extractCommitMessage(input)
        assertThat(result).isNull()
    }

    @Test
    fun `extractCommitMessage should extract commit message from valid tags`() {
        val input = """
            <aider-intention>
            Some intention text here
            </aider-intention>
            
            Some code changes here
            
            <aider-summary>
            Summary of changes
            </aider-summary>
            
            <aider-commit-message>
            feat: add new feature to application
            </aider-commit-message>
        """.trimIndent()
        
        val result = extractor.extractCommitMessage(input)
        assertThat(result).isEqualTo("feat: add new feature to application")
    }

    @Test
    fun `extractCommitMessage should handle multiline commit messages`() {
        val input = """
            <aider-commit-message>
            feat: add new feature to application
            
            This is a longer description of the feature.
            It spans multiple lines.
            </aider-commit-message>
        """.trimIndent()
        
        val result = extractor.extractCommitMessage(input)
        assertThat(result).isEqualTo("""
            feat: add new feature to application
            
            This is a longer description of the feature.
            It spans multiple lines.
        """.trimIndent())
    }

    @Test
    fun `extractCommitMessage should extract from augmented_output resource file`() {
        // Load the test data from the resource file
        val resourcePath = Paths.get("src", "test", "resources", "testdata", "augmented_output.txt")
        assertThat(Files.exists(resourcePath))
            .withFailMessage("Test resource file not found: ${resourcePath.toAbsolutePath()}")
            .isTrue()
            
        val input = Files.readString(resourcePath)
        
        val result = extractor.extractCommitMessage(input)
        assertThat(result).isEqualTo("feat: add Maven pom.xml equivalent to Gradle build")
    }

    @Test
    fun `extractCommitMessage should handle empty commit message tags`() {
        val input = """
            <aider-intention>Some intention</aider-intention>
            <aider-summary>Some summary</aider-summary>
            <aider-commit-message></aider-commit-message>
        """.trimIndent()
        
        val result = extractor.extractCommitMessage(input)
        assertThat(result).isNull()
    }

    @Test
    fun `extractCommitMessage should handle whitespace in commit message tags`() {
        val input = """
            <aider-commit-message>
                
            </aider-commit-message>
        """.trimIndent()
        
        val result = extractor.extractCommitMessage(input)
        assertThat(result).isNull()
    }

    @Test
    fun `extractCommitMessage should extract first commit message when multiple are present`() {
        val input = """
            <aider-commit-message>
            feat: first commit message
            </aider-commit-message>
            
            Some other content
            
            <aider-commit-message>
            fix: second commit message
            </aider-commit-message>
        """.trimIndent()
        
        val result = extractor.extractCommitMessage(input)
        assertThat(result).isEqualTo("feat: first commit message")
    }
}
