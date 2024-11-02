package de.andrena.codingaider.services

import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.ModelType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class TokenizerTest {
    @Test
    fun tokenize() {
        val registry: com.knuddels.jtokkit.api.EncodingRegistry? = Encodings.newLazyEncodingRegistry()
        val encodingForModel = registry?.getEncodingForModel(ModelType.GPT_4O)!!
        val result = encodingForModel.countTokens("Hello, world!")
        assertThat(result).isEqualTo(4)

    }
}