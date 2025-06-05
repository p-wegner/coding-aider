package de.andrena.codingaider.services

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.FileData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.nio.file.Files

class TodoPromptServiceTest {
    private lateinit var service: TodoPromptService
    private lateinit var mockProject: Project

    @BeforeEach
    fun setUp() {
        mockProject = mock(Project::class.java)
        service = TodoPromptService(mockProject)
    }

    @Test
    fun `collectTodos finds todos in files`() {
        val tempDir = Files.createTempDirectory("todoTest")
        val fileWithTodo = tempDir.resolve("A.kt").toFile().apply {
            writeText("""
                // TODO: fix this
                fun a() {}
            """.trimIndent())
        }
        val fileWithoutTodo = tempDir.resolve("B.kt").toFile().apply {
            writeText("fun b() {}")
        }
        val files = listOf(FileData(fileWithTodo.absolutePath, false), FileData(fileWithoutTodo.absolutePath, false))
        val result = service.collectTodos(files)

        assertThat(result).containsKey(fileWithTodo.absolutePath)
        assertThat(result[fileWithTodo.absolutePath]).isNotEmpty
        assertThat(result).doesNotContainKey(fileWithoutTodo.absolutePath)
    }

    @Test
    fun `buildPrompt appends todos to base prompt`() {
        val tempFile = Files.createTempFile("C", ".kt").toFile()
        tempFile.writeText("// TODO: implement")
        val files = listOf(FileData(tempFile.absolutePath, false))

        val result = service.buildPrompt("Base", files)

        assertThat(result).contains("Base")
        assertThat(result).contains("TODO: implement")
    }
}
