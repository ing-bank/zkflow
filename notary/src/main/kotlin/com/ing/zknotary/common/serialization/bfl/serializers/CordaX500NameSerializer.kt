package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.Serializable
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.CordaX500Name.Companion.MAX_LENGTH_LOCALITY
import net.corda.core.identity.CordaX500Name.Companion.MAX_LENGTH_ORGANISATION

object CordaX500NameSerializer : SurrogateSerializer<CordaX500Name, CordaX500NameSurrogate>(
    CordaX500NameSurrogate.serializer(),
    { CordaX500NameSurrogate.from(it) }
)

/**
 * Please note that we minimize the CordaX500Name as much as possible.
 * Only organisation, locality and country are preserved for minimal compatibility
 * TODO: validate that this is enough for most use cases with
 */
@Suppress("ArrayInDataClass")
@Serializable
data class CordaX500NameSurrogate(
    @FixedLength([MAX_LENGTH_ORGANISATION])
    val organisation: ByteArray,
    @FixedLength([MAX_LENGTH_LOCALITY])
    val locality: ByteArray,
    @FixedLength([LENGTH_COUNTRY])
    val country: ByteArray
) : Surrogate<CordaX500Name> {

    override fun toOriginal(): CordaX500Name {
        return CordaX500Name(
            String(organisation),
            String(locality),
            String(country)
        )
    }

    companion object {
        const val LENGTH_COUNTRY = 2
        const val SIZE =
            Int.SIZE_BYTES + MAX_LENGTH_ORGANISATION +
                Int.SIZE_BYTES + MAX_LENGTH_LOCALITY +
                Int.SIZE_BYTES + LENGTH_COUNTRY

        fun from(original: CordaX500Name) = with(original) {
            CordaX500NameSurrogate(
                organisation.toByteArray(Charsets.US_ASCII),
                locality.toByteArray(Charsets.US_ASCII),
                country.toByteArray(Charsets.US_ASCII)
            )
        }
    }
}
