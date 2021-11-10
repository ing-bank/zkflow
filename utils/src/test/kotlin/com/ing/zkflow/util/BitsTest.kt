package com.ing.zkflow.util

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class BitsTest {
    @Test
    fun `bitsToBytes should return 0 when 0 bits`() {
        0.bitsToBytes() shouldBe 0
    }

    @Test
    fun `bitsToBytes should return 1 when 1 bits`() {
        1.bitsToBytes() shouldBe 1
    }

    @Test
    fun `bitsToBytes should return 1 when 8 bits`() {
        8.bitsToBytes() shouldBe 1
    }

    @Test
    fun `bitsToBytes should return 8 when 60 bits`() {
        60.bitsToBytes() shouldBe 8
    }

    @Test
    fun `bitsToByteBoundary should return 0 when 0 bits`() {
        0.bitsToByteBoundary() shouldBe 0
    }

    @Test
    fun `bitsToByteBoundary should return 8 when 1 bits`() {
        1.bitsToByteBoundary() shouldBe 8
    }

    @Test
    fun `bitsToByteBoundary should return 8 when 8 bits`() {
        8.bitsToByteBoundary() shouldBe 8
    }

    @Test
    fun `bitsToByteBoundary should return 64 when 60 bits`() {
        60.bitsToByteBoundary() shouldBe 64
    }
}
