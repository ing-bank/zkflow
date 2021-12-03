package com.ing.zkflow.plugins.serialization.serializingobject

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CoordinatesTest {
    @Test
    fun `test numeric coordinate`() {
        val numeric = Coordinate.Numeric()

        numeric.next().toString() shouldBe "1"
        numeric.next(2).toString() shouldBe "2"
    }

    @Test
    fun `test literal coordinate`() {
        val literal = Coordinate.Literal()

        literal.toString() shouldBe "A"
        literal.next().toString() shouldBe "B"
        literal.next(2).toString() shouldBe "C"
        literal.next(25).toString() shouldBe "Z"
        literal.next(26).toString() shouldBe "AA"
        literal.next(27).toString() shouldBe "AB"
    }
}
