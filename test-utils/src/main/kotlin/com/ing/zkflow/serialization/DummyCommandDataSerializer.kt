package com.ing.zkflow.serialization

import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.testing.core.DummyCommandData

public object DummyCommandDataSerializer : FixedLengthKSerializerWithDefault<DummyCommandData> {
    override val descriptor: FixedLengthSerialDescriptor = FixedLengthSerialDescriptor(
        buildClassSerialDescriptor(DummyCommandData::class.qualifiedName ?: error("Cannot deduce qualified name")),
        0
    )
    override val default: DummyCommandData = DummyCommandData

    override fun deserialize(decoder: Decoder): DummyCommandData = default
    override fun serialize(encoder: Encoder, value: DummyCommandData): Unit = Unit
}
