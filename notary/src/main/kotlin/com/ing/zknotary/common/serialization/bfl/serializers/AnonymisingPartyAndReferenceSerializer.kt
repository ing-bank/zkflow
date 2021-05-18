package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.corda.core.contracts.PartyAndReference
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.utilities.OpaqueBytes

/**
 * Anonymising variant of the [PartyAndReferenceSerializer], reducing the serialized form.
 */
object AnonymisingPartyAndReferenceSerializer : KSerializer<PartyAndReference>
by (
    SurrogateSerializer(AnonymousPartyAndReferenceSurrogate.serializer()) {
        AnonymousPartyAndReferenceSurrogate(it.party.anonymise(), it.reference.copyBytes())
    }
    )

@Serializable
@Suppress("ArrayInDataClass")
data class AnonymousPartyAndReferenceSurrogate(
    val party: @Contextual AnonymousParty,
    @FixedLength([REFERENCE_SIZE])
    val reference: ByteArray
) : Surrogate<PartyAndReference> {
    override fun toOriginal(): PartyAndReference {
        return PartyAndReference(party, OpaqueBytes(reference))
    }

    companion object {
        // TODO why this size?
        const val REFERENCE_SIZE = PartyAndReferenceSurrogate.REFERENCE_SIZE
    }
}

fun AbstractParty.anonymise() = when (this) {
    is Party -> this.anonymise()
    is AnonymousParty -> this
    else -> error("Only supported instances of AbstractParty are Party and AnonymousParty")
}
