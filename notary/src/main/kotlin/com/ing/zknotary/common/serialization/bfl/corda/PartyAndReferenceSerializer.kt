package com.ing.zknotary.common.serialization.bfl.corda

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.corda.core.contracts.PartyAndReference
import net.corda.core.identity.AbstractParty
import net.corda.core.utilities.OpaqueBytes

@Serializable
@Suppress("ArrayInDataClass")
data class PartyAndReferenceSurrogate(
    val party: @Contextual AbstractParty,
    @FixedLength([256])
    val reference: ByteArray
) : Surrogate<PartyAndReference> {
    override fun toOriginal(): PartyAndReference {
        return PartyAndReference(party, OpaqueBytes(reference))
    }
}

object PartyAndReferenceSerializer : KSerializer<PartyAndReference>
    by (
        SurrogateSerializer(PartyAndReferenceSurrogate.serializer()) {
            PartyAndReferenceSurrogate(it.party, it.reference.copyBytes())
        }
        )
