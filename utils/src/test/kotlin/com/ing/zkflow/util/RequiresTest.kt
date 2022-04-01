package com.ing.zkflow.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class RequiresTest {
    @Test
    fun `require should return input when true`() {
        val actual = 3.require({ true }) {
            "Unexpected"
        }
        actual shouldBe 3
    }

    @Test
    fun `require should throw IllegalArgumentException when false`() {
        shouldThrow<IllegalArgumentException> {
            3.require({ false }) {
                "Expected"
            }
        }.message shouldBe "Expected"
    }

    @Test
    fun `requireNotEmpty should return input when not empty`() {
        val actual = listOf(1, 2, 3).requireNotEmpty {
            BUMMER
        }

        actual shouldContainInOrder listOf(1, 2, 3)
    }

    @Test
    fun `requireNotEmpty should throw exception when empty`() {
        val ex = shouldThrow<IllegalArgumentException> {
            emptyList<Unit>().requireNotEmpty {
                BUMMER
            }
        }
        ex.message shouldBe BUMMER
    }

    @Test
    fun `requireNotNull should return input when not null`() {
        val actual = 1.requireNotNull {
            BUMMER
        }

        actual shouldBe 1
    }

    @Test
    fun `requireNotNull should throw exception when null`() {
        val ex = shouldThrow<IllegalArgumentException> {
            null.requireNotNull<Int> {
                BUMMER
            }
        }
        ex.message shouldBe BUMMER
    }

    @Test
    fun `requireInstanceOf should return casted input when instance of required type`() {
        val value: Any = SOME_STRING
        val actual: String = value.requireInstanceOf {
            BUMMER
        }

        actual shouldBe value
    }

    @Test
    fun `requireInstanceOf should throw exception when not an instance of required type`() {
        val value: Any = SOME_STRING
        val ex = shouldThrow<IllegalArgumentException> {
            value.requireInstanceOf<Int> {
                BUMMER
            }
        }
        ex.message shouldBe BUMMER
    }

    companion object {
        private const val BUMMER = "Bummer"
        private const val SOME_STRING = "some string"
    }
}
