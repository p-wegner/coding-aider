package de.andrena.codingaider.utils

import com.intellij.openapi.project.Project
import de.andrena.codingaider.settings.AiderDefaults
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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
        assertThat(blocks).withFailMessage("Expected empty list for empty input").isEmpty()
    }

    @Test
    fun `parseBlocks should return empty list for input with no valid blocks`() {
        val input = "This is just some random text without any code blocks."
        val blocks = parser.parseBlocks(input)
        assertThat(blocks).withFailMessage("Expected empty list for non-matching input").isEmpty()
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
        assertThat(blocks).hasSize(1)
        val block = blocks[0]
        assertThat(block.filePath).isEqualTo("src/main/kotlin/App.kt")
        assertThat(block.language).isEqualTo("kotlin")
        assertThat(block.searchContent.trim()).isEqualTo("""
            fun main() {
            }
        """.trimIndent())
        assertThat(block.replaceContent.trim()).isEqualTo("""
            fun main() {
                println("Hello, World!")
            }
        """.trimIndent())
        assertThat(block.editType).isEqualTo(SearchReplaceBlockParser.EditType.SEARCH_REPLACE)
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
        assertThat(blocks).hasSize(1)
        val block = blocks[0]
        assertThat(block.filePath).isEqualTo("src/main/java/Main.java")
        assertThat(block.language).isEqualTo("java")
        assertThat(block.searchContent.trim()).isEqualTo("System.out.println(\"Old\");")
        assertThat(block.replaceContent.trim()).isEqualTo("System.out.println(\"New\");")
        assertThat(block.editType).isEqualTo(SearchReplaceBlockParser.EditType.SEARCH_REPLACE)
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
        assertThat(blocks).hasSize(1)
        val block = blocks[0]
        assertThat(block.filePath).isEqualTo("README.md")
        assertThat(block.language).isEqualTo("")
        assertThat(block.searchContent.trim()).isEqualTo("Old text")
        assertThat(block.replaceContent.trim()).isEqualTo("New text")
        assertThat(block.editType).isEqualTo(SearchReplaceBlockParser.EditType.SEARCH_REPLACE)
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
        assertThat(blocks).hasSize(1)
        val block = blocks[0]
        assertThat(block.filePath).isEqualTo("mathweb/flask/app.py")
        assertThat(block.language).isEqualTo("") // Diff-fenced doesn't capture language in this parser
        assertThat(block.searchContent.trim()).isEqualTo("from flask import Flask")
        assertThat(block.replaceContent.trim()).isEqualTo("""
            import math
            from flask import Flask
        """.trimIndent())
        assertThat(block.editType).isEqualTo(SearchReplaceBlockParser.EditType.SEARCH_REPLACE) // Parsed as SEARCH_REPLACE
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
        assertThat(blocks).hasSize(1)
        val block = blocks[0]
        assertThat(block.filePath).isEqualTo("show_greeting.py")
        assertThat(block.language).isEqualTo("") // Whole file doesn't capture language in this parser
        assertThat(block.searchContent).isEqualTo("") // No search content for whole file
        assertThat(block.replaceContent.trim()).isEqualTo("""
            import sys

            def greeting(name):
                print("Hey", name)

            if __name__ == '__main__':
                greeting(sys.argv[1])
        """.trimIndent())
        assertThat(block.editType).isEqualTo(SearchReplaceBlockParser.EditType.WHOLE_FILE)
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
        assertThat(blocks).hasSize(1)
        val block = blocks[0]
        assertThat(block.filePath).isEqualTo("config.txt")
        assertThat(block.language).isEqualTo("")
        assertThat(block.searchContent).isEqualTo("")
        assertThat(block.replaceContent.trim()).isEqualTo("""
            Setting=Value
            Another=Thing
        """.trimIndent())
        assertThat(block.editType).isEqualTo(SearchReplaceBlockParser.EditType.WHOLE_FILE)
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
        assertThat(blocks).hasSize(1)
        val block = blocks[0]
        assertThat(block.filePath).isEqualTo("mathweb/flask/app.py")
        assertThat(block.language).isEqualTo("") // Udiff doesn't have language
        assertThat(block.searchContent).isEqualTo("") // Udiff doesn't have search block
        assertThat(block.replaceContent.trim()).isEqualTo("""
            @@ ... @@
            -class MathWeb:
            +import sympy
            +
            +class MathWeb:
        """.trimIndent()) // Content is the diff itself
        assertThat(block.editType).isEqualTo(SearchReplaceBlockParser.EditType.UDIFF)
    }

    @Test
    fun `parseBlocks should handle mixed line endings CRLF`() {
        val input = "src/test.txt\r\n````\r\n<<<<<<< SEARCH\r\nOld Line\r\n=======\r\nNew Line\r\n>>>>>>> REPLACE\r\n````"
        val blocks = parser.parseBlocks(input)
        assertThat(blocks).hasSize(1)
        val block = blocks[0]
        assertThat(block.filePath).isEqualTo("src/test.txt")
        assertThat(block.searchContent.trim()).isEqualTo("Old Line")
        assertThat(block.replaceContent.trim()).isEqualTo("New Line")
        assertThat(block.editType).isEqualTo(SearchReplaceBlockParser.EditType.SEARCH_REPLACE)
    }

    @Test
    fun `parseBlocks should handle mixed line endings LF`() {
        val input = "src/test.txt\n````\n<<<<<<< SEARCH\nOld Line\n=======\nNew Line\n>>>>>>> REPLACE\n````"
        val blocks = parser.parseBlocks(input)
        assertThat(blocks).hasSize(1)
        val block = blocks[0]
        assertThat(block.filePath).isEqualTo("src/test.txt")
        assertThat(block.searchContent.trim()).isEqualTo("Old Line")
        assertThat(block.replaceContent.trim()).isEqualTo("New Line")
        assertThat(block.editType).isEqualTo(SearchReplaceBlockParser.EditType.SEARCH_REPLACE)
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
        assertThat(blocks).withFailMessage("Expected 3 blocks to be parsed").hasSize(3)

        // Block 1: Quadruple Search/Replace
        val block1 = blocks[0]
        assertThat(block1.filePath).isEqualTo("src/main/kotlin/App.kt")
        assertThat(block1.language).isEqualTo("kotlin")
        assertThat(block1.searchContent.trim()).isEqualTo("val x = 1")
        assertThat(block1.replaceContent.trim()).isEqualTo("val x = 2")
        assertThat(block1.editType).isEqualTo(SearchReplaceBlockParser.EditType.SEARCH_REPLACE)

        // Block 2: Whole File
        val block2 = blocks[1]
        assertThat(block2.filePath).isEqualTo("config.properties")
        assertThat(block2.language).isEqualTo("")
        assertThat(block2.searchContent).isEqualTo("")
        assertThat(block2.replaceContent.trim()).isEqualTo("key=value")
        assertThat(block2.editType).isEqualTo(SearchReplaceBlockParser.EditType.WHOLE_FILE)

        // Block 3: Udiff
        val block3 = blocks[2]
        assertThat(block3.filePath).isEqualTo("styles.css")
        assertThat(block3.language).isEqualTo("")
        assertThat(block3.searchContent).isEqualTo("")
        assertThat(block3.replaceContent).contains("+  color: blue;")
        assertThat(block3.editType).isEqualTo(SearchReplaceBlockParser.EditType.UDIFF)
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
        assertThat(blocks).withFailMessage("Expected 1 block after filtering instruction").hasSize(1)
        val block = blocks[0]
        assertThat(block.filePath).isEqualTo("src/code.js")
        assertThat(block.language).isEqualTo("javascript")
        assertThat(block.searchContent.trim()).isEqualTo("console.log('old');")
        assertThat(block.replaceContent.trim()).isEqualTo("console.log('new');")
        assertThat(block.editType).isEqualTo(SearchReplaceBlockParser.EditType.SEARCH_REPLACE)
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
        assertThat(blocks).hasSize(1)
        val block = blocks[0]
        assertThat(block.filePath).isEqualTo("src/new_file.txt")
        assertThat(block.language).isEqualTo("")
        assertThat(block.searchContent.trim()).withFailMessage("Search content should be empty for new file").isEqualTo("")
        assertThat(block.replaceContent.trim()).isEqualTo("""
            This is the content of the new file.
            It has multiple lines.
        """.trimIndent())
        assertThat(block.editType).isEqualTo(SearchReplaceBlockParser.EditType.SEARCH_REPLACE)
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
        assertThat(blocks).hasSize(1)
        val block = blocks[0]
        assertThat(block.filePath).isEqualTo("some/path/file.txt")
        assertThat(block.editType).isEqualTo(SearchReplaceBlockParser.EditType.SEARCH_REPLACE) // Should be SEARCH_REPLACE
        assertThat(block.searchContent.trim()).isEqualTo("This looks like a whole file block, but it's actually search/replace.")
        assertThat(block.replaceContent.trim()).isEqualTo("Replacement content.")
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
        assertThat(blocks).hasSize(1)
        val block = blocks[0]
        assertThat(block.filePath).isEqualTo("another/path.py")
        assertThat(block.editType).isEqualTo(SearchReplaceBlockParser.EditType.SEARCH_REPLACE) // Must be SEARCH_REPLACE
        assertThat(block.searchContent.trim()).isEqualTo("print(\"hello\")")
        assertThat(block.replaceContent.trim()).isEqualTo("print(\"world\")")
    }
    //TODO: this test is failing, no blocks are parsed
    @Test
    fun `parseBlocks should parse new file block from resource`() {
        // Load the test data from the resource file
        val resourcePath = Paths.get("src", "test", "resources", "testdata", "new_file_block.txt")
        assertThat(Files.exists(resourcePath)).withFailMessage("Test resource file not found: ${resourcePath.toAbsolutePath()}").isTrue()
        val input = Files.readString(resourcePath)

        val blocks = parser.parseBlocks(input)
        assertThat(blocks).withFailMessage("Expected one block to be parsed from the resource file").hasSize(1)

        val block = blocks[0]
        assertThat(block.filePath).isEqualTo("src/main/java/de/andrena/springai_demo/services/DefaultVotingService.java")
        assertThat(block.language).isEqualTo("java") // Language is captured by the triple-backtick regex
        assertThat(block.searchContent.trim()).withFailMessage("Search content should be empty for new file block").isEqualTo("")
        assertThat(block.replaceContent).withFailMessage("Replace content seems incorrect").contains("public class DefaultVotingService implements VotingService")
        assertThat(block.replaceContent).withFailMessage("Replace content seems incorrect").contains("package de.andrena.springai_demo.services;")
        assertThat(block.editType).withFailMessage("Edit type should be SEARCH_REPLACE for new file creation").isEqualTo(SearchReplaceBlockParser.EditType.SEARCH_REPLACE)
    }
}
