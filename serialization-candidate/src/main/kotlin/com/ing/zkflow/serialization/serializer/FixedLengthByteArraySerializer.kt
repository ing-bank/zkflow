package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

open class FixedLengthByteArraySerializer(maxSize: Int) : KSerializerWithDefault<ByteArray> {
    val strategy = FixedLengthListSerializer(maxSize, ByteSerializer)
    override val descriptor: SerialDescriptor = strategy.descriptor

    override val default = ByteArray(0)

    override fun serialize(encoder: Encoder, value: ByteArray) =
        strategy.serialize(encoder, value.toList())

    override fun deserialize(decoder: Decoder): ByteArray =
        strategy.deserialize(decoder).toByteArray()
}
