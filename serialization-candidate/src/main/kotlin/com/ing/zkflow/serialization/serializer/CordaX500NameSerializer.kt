package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.Surrogate
import com.ing.zkflow.serialization.serializer.string.FixedLengthUTF8StringSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.identity.CordaX500Name

class CordaX500NameSerializer : KSerializerWithDefault<CordaX500Name> {
    @Suppress("ArrayInDataClass", "ClassName")
    @Serializable
    private data class CordaX500NameSurrogate(
        val commonName: @Serializable(with = CommonNameSerializer_0::class) String?,
        val organisationUnit: @Serializable(with = OrganisationUnitSerializer_0::class) String?,
        val organisation: @Serializable(with = OrganisationSerializer_0::class) String,
        val locality: @Serializable(with = LocalitySerializer_0::class) String,
        val state: @Serializable(with = StateSerializer_0::class) String?,
        val country: @Serializable(with = CountrySerializer_0::class) String
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

    override val default = CordaX500Name(null, null, "", "", null, "")

    private val strategy = CordaX500NameSurrogate.serializer()
    override val descriptor: SerialDescriptor = strategy.descriptor

    override fun serialize(encoder: Encoder, value: CordaX500Name) = with(value) {
        encoder.encodeSerializableValue(strategy, CordaX500NameSurrogate(commonName, organisationUnit, organisation, locality, state, country))
    }

    override fun deserialize(decoder: Decoder): CordaX500Name =
        decoder.decodeSerializableValue(strategy).toOriginal()
}
