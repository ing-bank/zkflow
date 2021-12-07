package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthType
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

open class FixedLengthByteArraySerializer(maxSize: Int) : KSerializerWithDefault<ByteArray> {
    private val strategy = FixedLengthCollectionSerializer(maxSize, ByteSerializer, FixedLengthType.BYTE_ARRAY)
    override val descriptor: SerialDescriptor = strategy.descriptor

    override val default = ByteArray(0)

    override fun serialize(encoder: Encoder, value: ByteArray) =
        strategy.serialize(encoder, value.toList())

    override fun deserialize(decoder: Decoder): ByteArray =
        strategy.deserialize(decoder).toByteArray()
}
