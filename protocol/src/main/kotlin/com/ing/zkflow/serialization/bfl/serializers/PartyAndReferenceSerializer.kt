package com.ing.zkflow.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import net.corda.core.contracts.PartyAndReference
import net.corda.core.identity.AbstractParty
import net.corda.core.utilities.OpaqueBytes

@Serializable
@Suppress("ArrayInDataClass")
data class PartyAndReferenceSurrogate(
    val party: @Polymorphic AbstractParty,
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

object PartyAndReferenceSerializer :
    SurrogateSerializer<PartyAndReference, PartyAndReferenceSurrogate>(
        PartyAndReferenceSurrogate.serializer(),
        { PartyAndReferenceSurrogate(it.party, it.reference.copyBytes()) }
    )
