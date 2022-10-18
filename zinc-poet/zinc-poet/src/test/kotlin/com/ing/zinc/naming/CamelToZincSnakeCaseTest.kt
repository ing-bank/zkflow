package com.ing.zinc.naming

import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class CamelToZincSnakeCaseTest {
    @ParameterizedTest
    @CsvSource(
        "U16List2,u16_list_2",
        "U8ToI32Map4,u8_to_i32_map_4",
    )
    fun camelToZincSnakeCase_ShouldReturn(input: String, expected: String) {
        input.camelToZincSnakeCase() shouldBe expected
    }
}
