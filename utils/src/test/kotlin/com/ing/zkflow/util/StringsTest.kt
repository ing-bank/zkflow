package com.ing.zkflow.util

import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class StringsTest {
    @ParameterizedTest
    @CsvSource(
        "HELLO_WORLD,helloWorld",
        "hello_world,helloWorld",
        "HeLlO_wOrLd,helloWorld",
        "hello-world,helloWorld",
        "__a__,A",
    )
    fun `Snake case to camel case should work correctly`(input: String, expected: String) {
        val actual = input.snakeToCamelCase(false)
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
        val actual = input.snakeToCamelCase(true)
        actual shouldBe expected
    }
}
