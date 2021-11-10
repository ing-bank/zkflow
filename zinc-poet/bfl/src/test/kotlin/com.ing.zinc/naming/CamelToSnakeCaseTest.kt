package com.ing.zinc.naming

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class CamelToSnakeCaseTest {
    @ParameterizedTest
    @CsvSource(
        "camel,camel",
        "camelCase,camel_case",
        "EdDSAPublicKey,ed_dsa_public_key",
        "X509Certificate,x_509_certificate",
        "a_b,a_b",
        "___a,a",
        "a_b___,a_b",
        "100abc,100_abc",
        "U16List2,u16_list_2",
        "U8ToI32Map4,u8_to_i32_map_4",
    )
    fun camelToSnakeCase_ShouldReturn(input: String, expected: String) {
        input.camelToSnakeCase() shouldBe expected
    }

    @Test
    fun `camelToSnakeCase should fail for empty inputs`() {
        val ex = shouldThrow<IllegalArgumentException> {
            "".camelToSnakeCase()
        }
        ex.message shouldBe "Cannot generate a string from an empty part."
    }
}
