package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ExactLengthByteArraySerializerTest : SerializerTest {
    private val byteArray = listOf<Byte>(1, 2, 3).toByteArray()

    @ParameterizedTest
    @MethodSource("engines")
    fun `ByteArray must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(InstanceSerializer, byteArray) { a, b -> a.contentEquals(b) }
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `ByteArray-s must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(InstanceSerializer, byteArray).size shouldBe
            engine.serialize(InstanceSerializer, listOf<Byte>(3, 4, 5).toByteArray()).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with ByteArray must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsByteArray.serializer(), ContainsByteArray(byteArray)) { a, b -> a.innerByteArray.contentEquals(b.innerByteArray) }
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Should throw an exception when list too small`(engine: SerdeEngine) {
        val ex = shouldThrow<java.lang.IllegalArgumentException> {
            engine.serialize(InstanceSerializer, ByteArray(1))
        }
        ex.message shouldBe "Expected a collection of size 3, but got a collection of 1."
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Should throw an exception when list too large`(engine: SerdeEngine) {
        val ex = shouldThrow<java.lang.IllegalArgumentException> {
            engine.serialize(InstanceSerializer, ByteArray(5))
        }
        ex.message shouldBe "Expected a collection of size 3, but got a collection of 5."
    }

    @Suppress("ArrayInDataClass")
    @Serializable
    data class ContainsByteArray(
        @Serializable(with = InstanceSerializer::class)
        val innerByteArray: ByteArray
    )

    object InstanceSerializer : ExactLengthByteArraySerializer(3)
}
