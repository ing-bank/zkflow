package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.BFLEngine
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.toFixedLengthSerialDescriptorOrThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class FixedLengthListSerializerTest : SerializerTest {
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
            engine.serialize(InstanceSerializer, list + listOf(4, 5)).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with List(Int) must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsList.serializer(), ContainsList())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Sizes for List(Int) must match`(engine: BFLEngine) {
        val expectedBytesSize = (IntSerializer.descriptor.byteSize + MAX_SIZE * Int.SIZE_BYTES)

        InstanceSerializer.descriptor.byteSize shouldBe expectedBytesSize
        engine.serialize(InstanceSerializer, list).size shouldBe expectedBytesSize * engine.bytesScaler

        ContainsList.serializer().descriptor.toFixedLengthSerialDescriptorOrThrow().byteSize shouldBe expectedBytesSize
        engine.serialize(ContainsList.serializer(), ContainsList()).size shouldBe expectedBytesSize * engine.bytesScaler
    }

    @Serializable
    data class ContainsList(
        @Serializable(with = InstanceSerializer::class)
        val innerList: List<Int> = listOf(1, 2)
    )

    companion object {
        const val MAX_SIZE = 10
    }

    object InstanceSerializer : FixedLengthListSerializer<Int>(MAX_SIZE, IntSerializer)
}
