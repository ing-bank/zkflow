package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.BFLEngine
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.toFixedLengthSerialDescriptorOrThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ExactLengthListSerializerTest : SerializerTest {
    private val list = listOf(1, 2, 3)

    @ParameterizedTest
    @MethodSource("engines")
    fun `List(Int) must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(InstanceSerializer, list)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Lists of Ts must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(InstanceSerializer, list).size shouldBe
            engine.serialize(InstanceSerializer, listOf(3, 4, 5)).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with List(Int) must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsList.serializer(), ContainsList())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Sizes for List(Int) must match`(engine: BFLEngine) {
        val expectedBytesSize = MAX_SIZE * Int.SIZE_BYTES

        InstanceSerializer.descriptor.byteSize shouldBe expectedBytesSize
        engine.serialize(InstanceSerializer, list).size shouldBe expectedBytesSize * engine.bytesScaler

        ContainsList.serializer().descriptor.toFixedLengthSerialDescriptorOrThrow().byteSize shouldBe expectedBytesSize
        engine.serialize(ContainsList.serializer(), ContainsList()).size shouldBe expectedBytesSize * engine.bytesScaler
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Should throw an exception when list too small`(engine: BFLEngine) {
        val ex = shouldThrow<java.lang.IllegalArgumentException> {
            engine.serialize(InstanceSerializer, emptyList())
        }
        ex.message shouldBe "Expected a collection of size 3, but got a collection of 0."
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Should throw an exception when list too large`(engine: BFLEngine) {
        val ex = shouldThrow<java.lang.IllegalArgumentException> {
            engine.serialize(InstanceSerializer, List(5) { 13 })
        }
        ex.message shouldBe "Expected a collection of size 3, but got a collection of 5."
    }

    @Serializable
    data class ContainsList(
        @Serializable(with = InstanceSerializer::class)
        val innerList: List<Int> = listOf(1, 2, 3)
    )

    companion object {
        const val MAX_SIZE = 3
    }

    object InstanceSerializer : ExactLengthListSerializer<Int>(MAX_SIZE, IntSerializer)
}
