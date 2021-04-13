package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.identity.CordaX500Name

object CordaX500NameSerializer : KSerializer<CordaX500Name> {
    private val strategy = CordaX500NameSurrogate.serializer()
    override val descriptor: SerialDescriptor = strategy.descriptor

    override fun deserialize(decoder: Decoder): CordaX500Name {
        return decoder.decodeSerializableValue(strategy).toOriginal()
    }

    override fun serialize(encoder: Encoder, value: CordaX500Name) {
        encoder.encodeSerializableValue(strategy, CordaX500NameSurrogate.from(value))
    }
}

@Serializable
data class CordaX500NameSurrogate(
    @FixedLength([CordaX500Name.MAX_LENGTH_COMMON_NAME])
    val commonName: String?,
    @FixedLength([CordaX500Name.MAX_LENGTH_ORGANISATION_UNIT])
    val organisationUnit: String?,
    @FixedLength([CordaX500Name.MAX_LENGTH_ORGANISATION])
    val organisation: String,
    @FixedLength([CordaX500Name.MAX_LENGTH_LOCALITY])
    val locality: String,
    @FixedLength([CordaX500Name.MAX_LENGTH_STATE])
    val state: String?,
    // Country codes are defined in ISO 3166 and are all 2-letter abbreviations.
    @FixedLength([2])
    val country: String
) {

    fun toOriginal() = CordaX500Name(commonName, organisationUnit, organisation, locality, state, country)

    companion object {
        fun from(original: CordaX500Name) = with(original) {
            CordaX500NameSurrogate(commonName, organisationUnit, organisation, locality, state, country)
        }
    }
}
