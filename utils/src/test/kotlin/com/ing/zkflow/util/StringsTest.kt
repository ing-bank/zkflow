package com.ing.zkflow.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource

class StringsTest {
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

    @ParameterizedTest
    @CsvSource(
        "HELLO_WORLD,helloWorld",
        "hello_world,helloWorld",
        "HeLlO_wOrLd,helloWorld",
        "hello-world,helloWorld",
        "__a__,A",
    )
    fun `Snake case to camel case should work correctly`(input: String, expected: String) {
        val actual = input.snakeToCamelCase(capitalize = false)
        actual shouldBe expected
    }

    @ParameterizedTest
    @CsvSource(
        "HELLO_WORLD,HelloWorld",
        "hello_world,HelloWorld",
        "HeLlO_wOrLd,HelloWorld",
        "hello-world,HelloWorld",
        "__a__,A",
    )
    fun `Snake case to capitalized camel case should work correctly`(input: String, expected: String) {
        val actual = input.snakeToCamelCase(capitalize = true)
        actual shouldBe expected
    }

    @ParameterizedTest
    @MethodSource("splitBeforeFixtures")
    fun `splitBefore should work correctly`(input: String, marker: List<String>, expected: List<String>) {
        input.splitBefore(*marker.toTypedArray()) shouldBe expected
    }

    companion object {
        @JvmStatic
        fun splitBeforeFixtures() = listOf(
            Arguments.of("abcd", listOf("cd"), listOf("ab", "cd")), // normal split
            Arguments.of("abcd", listOf("ab"), listOf("abcd")), // split at beginning
            Arguments.of("abab", listOf("ab"), listOf("ab", "ab")), // consecutive splits
            Arguments.of("ab", listOf("ab"), listOf("ab")), // string equals marker
            Arguments.of("ab", listOf("cd"), listOf("ab")), // no match
            Arguments.of("abcabcabc", listOf("ab"), List(3) { "abc" }), // more than 2 elements
            Arguments.of("aa", listOf("a"), List(2) { "a" }), // single character matches
            Arguments.of("abcdef", listOf("cd", "ef"), listOf("ab", "cd", "ef")), // multiple markers
            Arguments.of("abcd", listOf("bcd", "cd"), listOf("a", "b", "cd")) // overlapping markers
        )
    }
}
