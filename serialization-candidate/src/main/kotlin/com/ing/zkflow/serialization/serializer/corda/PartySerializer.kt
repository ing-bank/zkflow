package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.serialization.serializer.KSerializerWithDefault
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party

open class PartySerializer(cordaSignatureId: Int, cordaX500NameSerializer: KSerializerWithDefault<CordaX500Name>) : KSerializerWithDefault<Party> {
    private val publicKeyStrategy = PublicKeySerializer(cordaSignatureId)
    private val cordaX500NameStrategy = cordaX500NameSerializer

    override val default = Party(cordaX500NameStrategy.default, publicKeyStrategy.default)

    // Impossible to use surrogates because respective serializing objects must be constructed from the received parameters,
    // i.e., `cordaSignatureId`, `cordaX500NameSerializer`.
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Party${publicKeyStrategy.algorithmNameIdentifier}") {
        element("cordaX500Name", cordaX500NameStrategy.descriptor)
        element("publicKey", publicKeyStrategy.descriptor)
    }

    override fun serialize(encoder: Encoder, value: Party) = with(encoder) {
        encodeSerializableValue(cordaX500NameStrategy, value.name)
        encodeSerializableValue(publicKeyStrategy, value.owningKey)
    }

    override fun deserialize(decoder: Decoder): Party = with(decoder) {
        Party(
            decodeSerializableValue(cordaX500NameStrategy),
            decodeSerializableValue(publicKeyStrategy),
        )
    }
}
