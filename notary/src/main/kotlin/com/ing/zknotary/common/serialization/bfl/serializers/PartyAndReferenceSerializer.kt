package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.corda.core.contracts.PartyAndReference
import net.corda.core.identity.AbstractParty
import net.corda.core.utilities.OpaqueBytes

object PartyAndReferenceSerializer : KSerializer<PartyAndReference>
by (
    SurrogateSerializer(PartyAndReferenceSurrogate.serializer()) {
        PartyAndReferenceSurrogate(it.party, it.reference.copyBytes())
    }
    )

@Serializable
@Suppress("ArrayInDataClass")
data class PartyAndReferenceSurrogate(
    val party: @Contextual AbstractParty,
    @FixedLength([REFERENCE_SIZE])
    val reference: ByteArray
) : Surrogate<PartyAndReference> {
    override fun toOriginal(): PartyAndReference {
        return PartyAndReference(party, OpaqueBytes(reference))
    }

    companion object {
        /**
         * Expect that the reference is a SecureHash, so use that as Reference size.
         */
        const val REFERENCE_SIZE = SecureHashSurrogate.BYTES_SIZE
    }
}
