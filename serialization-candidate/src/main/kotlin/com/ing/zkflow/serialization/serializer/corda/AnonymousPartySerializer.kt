package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.serialization.serializer.KSerializerWithDefault
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.identity.AnonymousParty

open class AnonymousPartySerializer(cordaSignatureId: Int) : KSerializerWithDefault<AnonymousParty> {
    private val strategy = PublicKeySerializer(cordaSignatureId)
    override val default = AnonymousParty(strategy.default)
    override val descriptor: SerialDescriptor = strategy.descriptor

    override fun serialize(encoder: Encoder, value: AnonymousParty) =
        encoder.encodeSerializableValue(strategy, value.owningKey)

    override fun deserialize(decoder: Decoder): AnonymousParty =
        AnonymousParty(decoder.decodeSerializableValue(strategy))
}
