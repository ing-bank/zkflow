package engine

import io.kotest.matchers.shouldBe
import kotlinx.serialization.KSerializer

interface SerdeEngine {
    fun <T> assertRoundTrip(strategy: KSerializer<T>, value: T, shouldPrint: Boolean = false, verify: ((T, T) -> Unit)?) {
        val de = deserialize(strategy, serialize(strategy, value, shouldPrint), shouldPrint)

        verify?.let { verify(value, de) } ?: de shouldBe value
    }

    fun <T> assertRoundTrip(strategy: KSerializer<T>, value: T, shouldPrint: Boolean = false) =
        assertRoundTrip(strategy, value, shouldPrint, null)

    fun <T> serialize(strategy: KSerializer<T>, value: T, shouldPrint: Boolean = false): ByteArray
    fun <T> deserialize(strategy: KSerializer<T>, data: ByteArray, shouldPrint: Boolean = false): T
}
