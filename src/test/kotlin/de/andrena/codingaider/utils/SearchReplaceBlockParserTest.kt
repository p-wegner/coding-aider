package de.andrena.codingaider.utils

import com.intellij.openapi.project.Project
import de.andrena.codingaider.settings.AiderDefaults
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.nio.file.Files
import java.nio.file.Paths

class SearchReplaceBlockParserTest {

    private lateinit var parser: SearchReplaceBlockParser
    private lateinit var mockProject: Project

    @BeforeEach
    fun setUp() {
        // Mock the Project dependency, as it's needed by the constructor
        // but not directly by the parseBlocks method itself.
        mockProject = mock(Project::class.java)
        parser = SearchReplaceBlockParser(mockProject)
    }

    @Test
    fun `parseBlocks should return empty list for empty input`() {
        val input = ""
        val blocks = parser.parseBlocks(input)
        assertTrue(blocks.isEmpty(), "Expected empty list for empty input")
    }

    @Test
    fun `parseBlocks should return empty list for input with no valid blocks`() {
        val input = "This is just some random text without any code blocks."
        val blocks = parser.parseBlocks(input)
        assertTrue(blocks.isEmpty(), "Expected empty list for non-matching input")
    }

    @Test
    fun `parseBlocks should parse quadruple backtick search replace block`() {
        val input = """
            src/main/kotlin/App.kt
            ````kotlin
            <<<<<<< SEARCH
            fun main() {
                println("Hello")
            }
            =======
            fun main() {
                println("Hello, World!")
            }
            >>>>>>> REPLACE
            ````
        """.trimIndent()
        val blocks = parser.parseBlocks(input)
        assertEquals(1, blocks.size)
        val block = blocks[0]
        assertEquals("src/main/kotlin/App.kt", block.filePath)
        assertEquals("kotlin", block.language)
        assertEquals("""
            fun main() {
                println("Hello")
            }
        """.trimIndent(), block.searchContent.trim())
        assertEquals("""
            fun main() {
                println("Hello, World!")
            }
        """.trimIndent(), block.replaceContent.trim())
        assertEquals(SearchReplaceBlockParser.EditType.SEARCH_REPLACE, block.editType)
    }

    @Test
    fun `parseBlocks should parse triple backtick search replace block with language`() {
        val input = """
            src/main/java/Main.java
            ```java
            <<<<<<< SEARCH
            System.out.println("Old");
            =======
            System.out.println("New");
            >>>>>>> REPLACE
            ```
        """.trimIndent()
        val blocks = parser.parseBlocks(input)
        assertEquals(1, blocks.size)
        val block = blocks[0]
        assertEquals("src/main/java/Main.java", block.filePath)
        assertEquals("java", block.language)
        assertEquals("System.out.println(\"Old\");", block.searchContent.trim())
        assertEquals("System.out.println(\"New\");", block.replaceContent.trim())
        assertEquals(SearchReplaceBlockParser.EditType.SEARCH_REPLACE, block.editType)
    }

    @Test
    fun `parseBlocks should parse triple backtick search replace block without language`() {
        val input = """
            README.md
            ```
            <<<<<<< SEARCH
            Old text
            =======
            New text
            >>>>>>> REPLACE
            ```
        """.trimIndent()
        val blocks = parser.parseBlocks(input)
        assertEquals(1, blocks.size)
        val block = blocks[0]
        assertEquals("README.md", block.filePath)
        assertEquals("", block.language)
        assertEquals("Old text", block.searchContent.trim())
        assertEquals("New text", block.replaceContent.trim())
        assertEquals(SearchReplaceBlockParser.EditType.SEARCH_REPLACE, block.editType)
    }

    @Test
    fun `parseBlocks should parse diff fenced block`() {
        val input = """
            ```
            mathweb/flask/app.py
            <<<<<<< SEARCH
            from flask import Flask
            =======
            import math
            from flask import Flask
            >>>>>>> REPLACE
            ```
        """.trimIndent()
        val blocks = parser.parseBlocks(input)
        assertEquals(1, blocks.size)
        val block = blocks[0]
        assertEquals("mathweb/flask/app.py", block.filePath)
        assertEquals("", block.language) // Diff-fenced doesn't capture language in this parser
        assertEquals("from flask import Flask", block.searchContent.trim())
        assertEquals("""
            import math
            from flask import Flask
        """.trimIndent(), block.replaceContent.trim())
        assertEquals(SearchReplaceBlockParser.EditType.SEARCH_REPLACE, block.editType) // Parsed as SEARCH_REPLACE
    }

    @Test
    fun `parseBlocks should parse whole file block`() {
        val input = """
            show_greeting.py
            ```python
            import sys

            def greeting(name):
                print("Hey", name)

            if __name__ == '__main__':
                greeting(sys.argv[1])
            ```
        """.trimIndent()
        val blocks = parser.parseBlocks(input)
        assertEquals(1, blocks.size)
        val block = blocks[0]
        assertEquals("show_greeting.py", block.filePath)
        assertEquals("", block.language) // Whole file doesn't capture language in this parser
        assertEquals("", block.searchContent) // No search content for whole file
        assertEquals("""
            import sys

            def greeting(name):
                print("Hey", name)

            if __name__ == '__main__':
                greeting(sys.argv[1])
        """.trimIndent(), block.replaceContent.trim())
        assertEquals(SearchReplaceBlockParser.EditType.WHOLE_FILE, block.editType)
    }

    @Test
    fun `parseBlocks should parse whole file block without language`() {
        val input = """
            config.txt
            ```
            Setting=Value
            Another=Thing
            ```
        """.trimIndent()
        val blocks = parser.parseBlocks(input)
        assertEquals(1, blocks.size)
        val block = blocks[0]
        assertEquals("config.txt", block.filePath)
        assertEquals("", block.language)
        assertEquals("", block.searchContent)
        assertEquals("""
            Setting=Value
            Another=Thing
        """.trimIndent(), block.replaceContent.trim())
        assertEquals(SearchReplaceBlockParser.EditType.WHOLE_FILE, block.editType)
    }

    @Test
    fun `parseBlocks should parse udiff block`() {
        val input = """
            ```diff
            --- mathweb/flask/app.py
            +++ mathweb/flask/app.py
            @@ ... @@
            -class MathWeb:
            +import sympy
            +
            +class MathWeb:
            ```
        """.trimIndent()
        val blocks = parser.parseBlocks(input)
        assertEquals(1, blocks.size)
        val block = blocks[0]
        assertEquals("mathweb/flask/app.py", block.filePath)
        assertEquals("", block.language) // Udiff doesn't have language
        assertEquals("", block.searchContent) // Udiff doesn't have search block
        assertEquals("""
            @@ ... @@
            -class MathWeb:
            +import sympy
            +
            +class MathWeb:
        """.trimIndent(), block.replaceContent.trim()) // Content is the diff itself
        assertEquals(SearchReplaceBlockParser.EditType.UDIFF, block.editType)
    }

    @Test
    fun `parseBlocks should handle mixed line endings CRLF`() {
        val input = "src/test.txt\r\n````\r\n<<<<<<< SEARCH\r\nOld Line\r\n=======\r\nNew Line\r\n>>>>>>> REPLACE\r\n````"
        val blocks = parser.parseBlocks(input)
        assertEquals(1, blocks.size)
        val block = blocks[0]
        assertEquals("src/test.txt", block.filePath)
        assertEquals("Old Line", block.searchContent.trim())
        assertEquals("New Line", block.replaceContent.trim())
        assertEquals(SearchReplaceBlockParser.EditType.SEARCH_REPLACE, block.editType)
    }

    @Test
    fun `parseBlocks should handle mixed line endings LF`() {
        val input = "src/test.txt\n````\n<<<<<<< SEARCH\nOld Line\n=======\nNew Line\n>>>>>>> REPLACE\n````"
        val blocks = parser.parseBlocks(input)
        assertEquals(1, blocks.size)
        val block = blocks[0]
        assertEquals("src/test.txt", block.filePath)
        assertEquals("Old Line", block.searchContent.trim())
        assertEquals("New Line", block.replaceContent.trim())
        assertEquals(SearchReplaceBlockParser.EditType.SEARCH_REPLACE, block.editType)
    }

    @Test
    fun `parseBlocks should parse multiple blocks of different types`() {
        val input = """
            First file change:
            src/main/kotlin/App.kt
            ````kotlin
            <<<<<<< SEARCH
            val x = 1
            =======
            val x = 2
            >>>>>>> REPLACE
            ````

            Second file change (whole file):
            config.properties
            ```
            key=value
            ```

            Third file change (udiff):
            ```diff
            --- styles.css
            +++ styles.css
            @@ -1,3 +1,4 @@
             body {
            -  color: black;
            +  color: blue;
            +  font-size: 16px;
             }
            ```
        """.trimIndent()

        val blocks = parser.parseBlocks(input)
        assertEquals(3, blocks.size, "Expected 3 blocks to be parsed")

        // Block 1: Quadruple Search/Replace
        val block1 = blocks[0]
        assertEquals("src/main/kotlin/App.kt", block1.filePath)
        assertEquals("kotlin", block1.language)
        assertEquals("val x = 1", block1.searchContent.trim())
        assertEquals("val x = 2", block1.replaceContent.trim())
        assertEquals(SearchReplaceBlockParser.EditType.SEARCH_REPLACE, block1.editType)

        // Block 2: Whole File
        val block2 = blocks[1]
        assertEquals("config.properties", block2.filePath)
        assertEquals("", block2.language)
        assertEquals("", block2.searchContent)
        assertEquals("key=value", block2.replaceContent.trim())
        assertEquals(SearchReplaceBlockParser.EditType.WHOLE_FILE, block2.editType)

        // Block 3: Udiff
        val block3 = blocks[2]
        assertEquals("styles.css", block3.filePath)
        assertEquals("", block3.language)
        assertEquals("", block3.searchContent)
        assertTrue(block3.replaceContent.contains("+  color: blue;"))
        assertEquals(SearchReplaceBlockParser.EditType.UDIFF, block3.editType)
    }

    @Test
    fun `parseBlocks should ignore instruction prompt`() {
        val instruction = AiderDefaults.PLUGIN_BASED_EDITS_INSTRUCTION
        val input = """
            $instruction

            Here are the changes:
            src/code.js
            ```javascript
            <<<<<<< SEARCH
            console.log('old');
            =======
            console.log('new');
            >>>>>>> REPLACE
            ```
        """.trimIndent()

        val blocks = parser.parseBlocks(input)
        assertEquals(1, blocks.size, "Expected 1 block after filtering instruction")
        val block = blocks[0]
        assertEquals("src/code.js", block.filePath)
        assertEquals("javascript", block.language)
        assertEquals("console.log('old');", block.searchContent.trim())
        assertEquals("console.log('new');", block.replaceContent.trim())
        assertEquals(SearchReplaceBlockParser.EditType.SEARCH_REPLACE, block.editType)
    }

    @Test
    fun `parseBlocks should correctly handle new file creation block`() {
        val input = """
            src/new_file.txt
            ````
            <<<<<<< SEARCH
            =======
            This is the content of the new file.
            It has multiple lines.
            >>>>>>> REPLACE
            ````
        """.trimIndent()
        val blocks = parser.parseBlocks(input)
        assertEquals(1, blocks.size)
        val block = blocks[0]
        assertEquals("src/new_file.txt", block.filePath)
        assertEquals("", block.language)
        assertEquals("", block.searchContent.trim(), "Search content should be empty for new file")
        assertEquals("""
            This is the content of the new file.
            It has multiple lines.
        """.trimIndent(), block.replaceContent.trim())
        assertEquals(SearchReplaceBlockParser.EditType.SEARCH_REPLACE, block.editType)
    }

    @Test
    fun `parseBlocks should not parse whole file block if it contains search replace markers`() {
        // This looks like a whole file block but contains markers, so WHOLE_REGEX should ignore it
        // and one of the SEARCH_REPLACE regexes should pick it up instead.
        val input = """
            some/path/file.txt
            ```
            <<<<<<< SEARCH
            This looks like a whole file block, but it's actually search/replace.
            =======
            Replacement content.
            >>>>>>> REPLACE
            ```
        """.trimIndent()
        val blocks = parser.parseBlocks(input)
        assertEquals(1, blocks.size)
        val block = blocks[0]
        assertEquals("some/path/file.txt", block.filePath)
        assertEquals(SearchReplaceBlockParser.EditType.SEARCH_REPLACE, block.editType) // Should be SEARCH_REPLACE
        assertEquals("This looks like a whole file block, but it's actually search/replace.", block.searchContent.trim())
        assertEquals("Replacement content.", block.replaceContent.trim())
    }

    @Test
    fun `parseBlocks should prioritize specific search replace formats over whole file`() {
        // This input matches both WHOLE_REGEX and SIMPLE_TRIPLE_REGEX
        // The parser should prioritize SIMPLE_TRIPLE_REGEX because it's more specific.
        val input = """
            another/path.py
            ```
            <<<<<<< SEARCH
            print("hello")
            =======
            print("world")
            >>>>>>> REPLACE
            ```
        """.trimIndent()
        val blocks = parser.parseBlocks(input)
        assertEquals(1, blocks.size)
        val block = blocks[0]
        assertEquals("another/path.py", block.filePath)
        assertEquals(SearchReplaceBlockParser.EditType.SEARCH_REPLACE, block.editType) // Must be SEARCH_REPLACE
        assertEquals("print(\"hello\")", block.searchContent.trim())
        assertEquals("print(\"world\")", block.replaceContent.trim())
    }

    @Test
    fun `parseBlocks should parse new file block from resource`() {
        // Load the test data from the resource file
        val resourcePath = Paths.get("src", "test", "resources", "testdata", "new_file_block.txt")
        assertTrue(Files.exists(resourcePath), "Test resource file not found: ${resourcePath.toAbsolutePath()}")
        val input = Files.readString(resourcePath)

        val blocks = parser.parseBlocks(input)
        assertEquals(1, blocks.size, "Expected one block to be parsed from the resource file")

        val block = blocks[0]
        assertEquals("src/main/java/de/andrena/springai_demo/services/DefaultVotingService.java", block.filePath)
        assertEquals("java", block.language) // Language is captured by the triple-backtick regex
        assertEquals("", block.searchContent.trim(), "Search content should be empty for new file block")
        assertTrue(block.replaceContent.contains("public class DefaultVotingService implements VotingService"), "Replace content seems incorrect")
        assertTrue(block.replaceContent.contains("package de.andrena.springai_demo.services;"), "Replace content seems incorrect")
        assertEquals(SearchReplaceBlockParser.EditType.SEARCH_REPLACE, block.editType, "Edit type should be SEARCH_REPLACE for new file creation")
    }
}
