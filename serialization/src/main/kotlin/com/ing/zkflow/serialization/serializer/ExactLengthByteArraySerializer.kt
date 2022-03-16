package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthSerialDescriptor
import com.ing.zkflow.serialization.FixedLengthType
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

open class ExactLengthByteArraySerializer(exactSize: Int) : FixedLengthKSerializerWithDefault<ByteArray> {
    private val strategy = ExactLengthCollectionSerializer(exactSize, ByteSerializer, FixedLengthType.EXACT_BYTE_ARRAY)
    override val descriptor: FixedLengthSerialDescriptor = strategy.descriptor

    override val default = ByteArray(exactSize)

    override fun serialize(encoder: Encoder, value: ByteArray) =
        strategy.serialize(encoder, value.toList())

    override fun deserialize(decoder: Decoder): ByteArray =
        strategy.deserialize(decoder).toByteArray()
}
