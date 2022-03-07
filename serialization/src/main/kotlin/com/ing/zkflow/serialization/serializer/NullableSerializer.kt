package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthSerialDescriptor
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Every nullable type must be serialized with an object inheriting from this class.
 */
abstract class NullableSerializer<T>(valueSerializer: FixedLengthKSerializerWithDefault<T>) :
    FixedLengthKSerializerWithDefault<T?> {
    @Serializable
    private data class NullableValue<T>(
        val isNull: Boolean,
        val value: T
    )

    override val default = valueSerializer.default

    private val strategy = NullableValue.serializer(valueSerializer)
    override val descriptor = FixedLengthSerialDescriptor(
        strategy.descriptor.nullable,
        BooleanSerializer.descriptor.byteSize + valueSerializer.descriptor.byteSize
    )

    override fun serialize(encoder: Encoder, value: T?) =
        encoder.encodeSerializableValue(strategy, NullableValue(value == null, value ?: default))

    override fun deserialize(decoder: Decoder): T? =
        decoder.decodeSerializableValue(strategy).let {
            if (it.isNull) null else it.value
        }
}
