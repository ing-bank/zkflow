package com.ing.zkflow.serialization.engine

import io.kotest.matchers.shouldBe
import kotlinx.serialization.KSerializer

public interface SerdeEngine {
    public fun <T> assertRoundTrip(strategy: KSerializer<T>, value: T, shouldPrint: Boolean = false, verify: ((T, T) -> Unit)?) {
        val de = deserialize(strategy, serialize(strategy, value, shouldPrint), shouldPrint)

        verify?.let { verify(value, de) } ?: de shouldBe value
    }

    public fun <T> assertRoundTrip(strategy: KSerializer<T>, value: T, shouldPrint: Boolean = false): Unit =
        assertRoundTrip(strategy, value, shouldPrint, null)

    public fun <T> serialize(strategy: KSerializer<T>, value: T, shouldPrint: Boolean = false): ByteArray
    public fun <T> deserialize(strategy: KSerializer<T>, data: ByteArray, shouldPrint: Boolean = false): T
}
