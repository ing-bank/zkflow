package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.BFLEngine
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.toFixedLengthSerialDescriptorOrThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class FixedLengthMapSerializerTest : SerializerTest {
    private val map = mapOf<Int, Short>(1 to 12, 2 to 22, 3 to 32)

    @ParameterizedTest
    @MethodSource("engines")
    fun `Map(Int, Short) must be serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(InstanceSerializer, map)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Maps (Int, Short) must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(InstanceSerializer, map).size shouldBe
            engine.serialize(InstanceSerializer, map + mapOf(4 to 42)).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with Map(Int, Short) must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsMap.serializer(), ContainsMap())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Sizes for Map(Int, Short) must match`(engine: BFLEngine) {
        val expectedBytesSize = (IntSerializer.descriptor.byteSize + MAX_SIZE * (Int.SIZE_BYTES + Short.SIZE_BYTES))

        InstanceSerializer.descriptor.byteSize shouldBe expectedBytesSize
        engine.serialize(InstanceSerializer, map).size shouldBe expectedBytesSize * engine.bytesScaler

        ContainsMap.serializer().descriptor.toFixedLengthSerialDescriptorOrThrow().byteSize shouldBe expectedBytesSize
        engine.serialize(ContainsMap.serializer(), ContainsMap()).size shouldBe expectedBytesSize * engine.bytesScaler
    }

    @Serializable
    data class ContainsMap(
        @Serializable(with = InstanceSerializer::class)
        val innerMap: Map<Int, Short> = mapOf(1 to 12, 2 to 22)
    )

    companion object {
        const val MAX_SIZE = 10
    }

    object InstanceSerializer : FixedLengthMapSerializer<Int, Short>(MAX_SIZE, IntSerializer, ShortSerializer)
}
