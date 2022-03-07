package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.Surrogate
import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.serializer.NullableSerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthUTF8StringSerializer
import com.ing.zkflow.serialization.toFixedLengthSerialDescriptorOrThrow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.identity.CordaX500Name

object CordaX500NameSerializer : FixedLengthKSerializerWithDefault<CordaX500Name> {
    @Suppress("ArrayInDataClass", "ClassName")
    @Serializable
    private data class CordaX500NameSurrogate(
        @Serializable(with = CommonNameSerializer_0::class) val commonName: String?,
        @Serializable(with = OrganisationUnitSerializer_0::class) val organisationUnit: String?,
        @Serializable(with = OrganisationSerializer_0::class) val organisation: String,
        @Serializable(with = LocalitySerializer_0::class) val locality: String,
        @Serializable(with = StateSerializer_0::class) val state: String?,
        @Serializable(with = CountrySerializer_0::class) val country: String
    ) : Surrogate<CordaX500Name> {

        object CommonNameSerializer_0 : NullableSerializer<String>(CommonNameSerializer_1)
        object CommonNameSerializer_1 : FixedLengthUTF8StringSerializer(CordaX500Name.MAX_LENGTH_COMMON_NAME)

        object OrganisationUnitSerializer_0 : NullableSerializer<String>(OrganisationUnitSerializer_1)
        object OrganisationUnitSerializer_1 : FixedLengthUTF8StringSerializer(CordaX500Name.MAX_LENGTH_ORGANISATION_UNIT)

        object OrganisationSerializer_0 : FixedLengthUTF8StringSerializer(CordaX500Name.MAX_LENGTH_ORGANISATION)

        object LocalitySerializer_0 : FixedLengthUTF8StringSerializer(CordaX500Name.MAX_LENGTH_LOCALITY)

        object StateSerializer_0 : NullableSerializer<String>(StateSerializer_1)
        object StateSerializer_1 : FixedLengthUTF8StringSerializer(CordaX500Name.MAX_LENGTH_STATE)

        object CountrySerializer_0 : FixedLengthUTF8StringSerializer(LENGTH_COUNTRY)

        override fun toOriginal() =
            CordaX500Name(commonName, organisationUnit, organisation, locality, state, country)

        companion object {
            const val LENGTH_COUNTRY = 2
        }
    }

    // `organization` value must contain at least two characters.
    // `country` value must be a valid country code.
    override val default = CordaX500Name(null, null, "XX", "", null, "NL")

    private val strategy = CordaX500NameSurrogate.serializer()
    override val descriptor = strategy.descriptor.toFixedLengthSerialDescriptorOrThrow()

    override fun serialize(encoder: Encoder, value: CordaX500Name) = with(value) {
        encoder.encodeSerializableValue(strategy, CordaX500NameSurrogate(commonName, organisationUnit, organisation, locality, state, country))
    }

    override fun deserialize(decoder: Decoder): CordaX500Name =
        decoder.decodeSerializableValue(strategy).toOriginal()
}
