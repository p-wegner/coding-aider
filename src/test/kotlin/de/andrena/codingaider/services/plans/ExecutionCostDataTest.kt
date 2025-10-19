package de.andrena.codingaider.services.plans

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ExecutionCostDataTest {

    @Test
    fun `fromCommandOutput should extract model from Main model format`() {
        val output = """
            > Main model: o3-mini with diff edit format
            > Weak model: gpt-4o-mini
            > Git repo: .git with 22 files
            > Tokens: 2.3k sent, 75 received. Cost: ${'$'}0.0029 message, ${'$'}0.0029 session.
        """.trimIndent()

        val costData = ExecutionCostData.fromCommandOutput(output)

        assertThat(costData.model).isEqualTo("o3-mini")
        assertThat(costData.tokensSent).isEqualTo(2300)
        assertThat(costData.tokensReceived).isEqualTo(75)
        assertThat(costData.messageCost).isCloseTo(0.0029, org.assertj.core.api.Assertions.offset(0.0001))
        assertThat(costData.sessionCost).isCloseTo(0.0029, org.assertj.core.api.Assertions.offset(0.0001))
    }

    @Test
    fun `fromCommandOutput should extract model from Model format with additional details`() {
        val output = """
            > Model: deepseek/deepseek-chat with diff edit format, infinite output
            > Git repo: .git with 98 files
            > Tokens: 3.9k sent, 264 received. Cost: ${'$'}0.00062 message, ${'$'}0.00062 session.
        """.trimIndent()

        val costData = ExecutionCostData.fromCommandOutput(output)

        assertThat(costData.model).isEqualTo("deepseek/deepseek-chat")
        assertThat(costData.tokensSent).isEqualTo(3900)
        assertThat(costData.tokensReceived).isEqualTo(264)
        assertThat(costData.messageCost).isCloseTo(0.00062, org.assertj.core.api.Assertions.offset(0.00001))
        assertThat(costData.sessionCost).isCloseTo(0.00062, org.assertj.core.api.Assertions.offset(0.00001))
    }

    @Test
    fun `fromCommandOutput should extract model from standard Model format`() {
        val output = """
            > Model: claude-3-5-haiku-20241022 with diff edit format
            > Git repo: .git with 22 files
            > Tokens: 1.5k sent, 100 received. Cost: ${'$'}0.001 message, ${'$'}0.001 session.
        """.trimIndent()

        val costData = ExecutionCostData.fromCommandOutput(output)

        assertThat(costData.model).isEqualTo("claude-3-5-haiku-20241022")
        assertThat(costData.tokensSent).isEqualTo(1500)
        assertThat(costData.tokensReceived).isEqualTo(100)
        assertThat(costData.messageCost).isCloseTo(0.001, org.assertj.core.api.Assertions.offset(0.0001))
        assertThat(costData.sessionCost).isCloseTo(0.001, org.assertj.core.api.Assertions.offset(0.0001))
    }

    @Test
    fun `fromCommandOutput should handle fallback when no model is found`() {
        val output = """
            > Git repo: .git with 22 files
            > Tokens: 1.5k sent, 100 received. Cost: ${'$'}0.001 message, ${'$'}0.001 session.
        """.trimIndent()

        val costData = ExecutionCostData.fromCommandOutput(output)

        assertThat(costData.model).isEqualTo("unknown-model")
    }

    @Test
    fun `fromCommandOutput should extract summary from aider-summary tags`() {
        val output = """
            > Model: claude-3-5-haiku-20241022 with diff edit format
            > Git repo: .git with 22 files
            > Tokens: 1.5k sent, 100 received. Cost: ${'$'}0.001 message, ${'$'}0.001 session.
            <aider-summary>Fixed the model detection regex to handle multiple output formats</aider-summary>
        """.trimIndent()

        val costData = ExecutionCostData.fromCommandOutput(output)

        assertThat(costData.model).isEqualTo("claude-3-5-haiku-20241022")
        assertThat(costData.summary).isEqualTo("Fixed the model detection regex to handle multiple output formats")
    }
}