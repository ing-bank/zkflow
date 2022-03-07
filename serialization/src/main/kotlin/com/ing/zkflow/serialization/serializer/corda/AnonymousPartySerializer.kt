package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.toFixedLengthSerialDescriptorOrThrow
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.identity.AnonymousParty

open class AnonymousPartySerializer(cordaSignatureId: Int) : FixedLengthKSerializerWithDefault<AnonymousParty> {
    private val strategy = PublicKeySerializer(cordaSignatureId)
    override val default = AnonymousParty(strategy.default)

    override val descriptor = buildClassSerialDescriptor("AnonymousParty${strategy.algorithmNameIdentifier}") {
        element("publicKey", strategy.descriptor)
    }.toFixedLengthSerialDescriptorOrThrow()

    override fun serialize(encoder: Encoder, value: AnonymousParty) =
        encoder.encodeSerializableValue(strategy, value.owningKey)

    override fun deserialize(decoder: Decoder): AnonymousParty =
        AnonymousParty(decoder.decodeSerializableValue(strategy))
}
