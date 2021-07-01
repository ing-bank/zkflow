package io.ivno.collateraltoken.serialization

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import io.onixlabs.corda.identityframework.contract.Attestation
import io.onixlabs.corda.identityframework.contract.AttestationPointer
import io.onixlabs.corda.identityframework.contract.AttestationStatus
import kotlinx.serialization.Contextual
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty

object AttestationSerializer : SurrogateSerializer<Attestation<*>, AttestationSurrogate>(
    AttestationSurrogate.serializer(),
    {
        AttestationSurrogate(
            it.attestor,
            it.attestees,
            it.pointer,
            it.status,
            it.metadata,
            it.linearId,
            it.previousStateRef
        )
    }
)

@Serializable
data class AttestationSurrogate(
    val attestor: @Polymorphic AbstractParty,
    @FixedLength([ATTESTEES_SIZE])
    val attestees: Set<@Polymorphic AbstractParty>,
    val pointer: @Contextual AttestationPointer<*>,
    val status: AttestationStatus,
    @FixedLength([METADATA_MAP_SIZE, METADATA_KEY_LENGTH, METADATA_VALUE_LENGTH])
    val metadata: Map<String, String>,
    val linearId: @Contextual UniqueIdentifier,
    val previousStateRef: @Contextual StateRef?
) : Surrogate<Attestation<*>> {
    override fun toOriginal(): Attestation<*> {
        return Attestation(
            attestor, attestees, pointer, status, metadata, linearId, previousStateRef
        )
    }

    companion object {
        const val ATTESTEES_SIZE = 2
        const val METADATA_MAP_SIZE = 5
        const val METADATA_KEY_LENGTH = 32
        const val METADATA_VALUE_LENGTH = 128
    }
}
