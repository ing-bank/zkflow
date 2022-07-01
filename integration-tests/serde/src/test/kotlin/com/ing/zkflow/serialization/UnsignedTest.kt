package com.ing.zkflow.serialization

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.generated.UnsignedSerializer
import com.ing.zkflow.serialization.serializer.ByteSerializer
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.LongSerializer
import com.ing.zkflow.serialization.serializer.ShortSerializer
import com.ing.zkflow.serialization.serializer.UByteSerializer
import com.ing.zkflow.serialization.serializer.UIntSerializer
import com.ing.zkflow.serialization.serializer.ULongSerializer
import com.ing.zkflow.serialization.serializer.UShortSerializer
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class UnsignedTest : SerializerTest {
    // Setup
    @ZKP
    data class Unsigned(
        val byte: Byte = -1,
        val ubyte: UByte = 1U,
        val short: Short = -1,
        val ushort: UShort = 1U,
        val int: Int = -1,
        val uint: UInt = 1U,
        val long: Long = -1,
        val ulong: ULong = 1U,
    )

    // Resolved
    @Suppress("ClassName")
    @Serializable
    data class UnsignedResolved(
        @Serializable(with = Byte_0::class)
        val byte: @Contextual Byte = -1,
        @Serializable(with = UByte_0::class)
        val ubyte: @Contextual UByte = 1U,
        @Serializable(with = Short_0::class)
        val short: @Contextual Short = -1,
        @Serializable(with = UShort_0::class)
        val ushort: @Contextual UShort = 1U,
        @Serializable(with = Int_0::class)
        val int: @Contextual Int = -1,
        @Serializable(with = UInt_0::class)
        val uint: @Contextual UInt = 1U,
        @Serializable(with = Long_0::class)
        val long: @Contextual Long = -1,
        @Serializable(with = ULong_0::class)
        val ulong: @Contextual ULong = 1U,
    ) {

        object Byte_0 : WrappedFixedLengthKSerializerWithDefault<Byte>(ByteSerializer)
        object UByte_0 : WrappedFixedLengthKSerializerWithDefault<UByte>(UByteSerializer)
        object Short_0 : WrappedFixedLengthKSerializerWithDefault<Short>(ShortSerializer)
        object UShort_0 : WrappedFixedLengthKSerializerWithDefault<UShort>(UShortSerializer)
        object Int_0 : WrappedFixedLengthKSerializerWithDefault<Int>(IntSerializer)
        object UInt_0 : WrappedFixedLengthKSerializerWithDefault<UInt>(UIntSerializer)
        object Long_0 : WrappedFixedLengthKSerializerWithDefault<Long>(LongSerializer)
        object ULong_0 : WrappedFixedLengthKSerializerWithDefault<ULong>(ULongSerializer)
    }

    // Tests
    @ParameterizedTest
    @MethodSource("engines")
    fun `Unsigned makes a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(UnsignedSerializer, Unsigned())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Unsigned generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            UnsignedResolved.serializer(),
            UnsignedResolved()
        ) shouldBe
            engine.serialize(UnsignedSerializer, Unsigned())
    }
}
