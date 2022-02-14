package com.ing.zkflow.resolved

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
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
import kotlinx.serialization.builtins.serializer
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Suppress("ClassName")
@Serializable
data class WrapsUnsigned(
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

class WrapsUnsignedTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsBasic make a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            com.ing.zkflow.annotated.WrapsUnsigned.serializer(),
            com.ing.zkflow.annotated.WrapsUnsigned(),
            true
        )
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsBasic generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            com.ing.zkflow.annotated.WrapsUnsigned.serializer(),
            com.ing.zkflow.annotated.WrapsUnsigned(),
            true
        ) shouldBe
            engine.serialize(WrapsUnsigned.serializer(), WrapsUnsigned())
    }
}
