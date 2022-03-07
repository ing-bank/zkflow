package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthSerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.testing.core.DummyCommandData

object DummyCommandDataSerializer : FixedLengthKSerializerWithDefault<DummyCommandData> {
    override val descriptor = FixedLengthSerialDescriptor(
        buildClassSerialDescriptor(DummyCommandData::class.qualifiedName ?: error("Cannot deduce qualified name")),
        0
    )
    override val default = DummyCommandData

    override fun deserialize(decoder: Decoder) = default
    override fun serialize(encoder: Encoder, value: DummyCommandData) = Unit
}
