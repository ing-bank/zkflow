package io.ivno.collateraltoken.serialization

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import io.onixlabs.corda.bnms.contract.Network
import io.onixlabs.corda.bnms.contract.membership.Membership
import io.onixlabs.corda.bnms.contract.membership.MembershipAttestation
import io.onixlabs.corda.identityframework.contract.Attestation
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party

object MembershipAttestationSerializer: SurrogateSerializer<MembershipAttestation, MembershipAttestationSurrogate>(
    MembershipAttestationSurrogate.serializer(),
    {
        MembershipAttestationSurrogate(
            network = it.network,
            attestation = Attestation(
                it.attestor,
                it.attestees,
                it.pointer,
                it.status,
                it.metadata,
                it.linearId,
                it.previousStateRef
            )
        )
    }
)

@Serializable
data class MembershipAttestationSurrogate(
    val network: @Contextual Network,
    val attestation: @Contextual Attestation<@Contextual Membership>
): Surrogate<MembershipAttestation> {
    override fun toOriginal(): MembershipAttestation {
        val membership = Membership(
            network = network,
            holder = attestation.attestees.single(),
            linearId = getMembershipLinearId(),
        )
        val bogusNotary = Party(
            CordaX500Name.parse(BOGUS_NOTARY_NAME),
            attestation.attestor.owningKey
        )
        val membershipState: StateAndRef<Membership> = StateAndRef(
            TransactionState(
                data = membership,
                contract = BOGUS_CONTRACT_NAME,
                notary = bogusNotary
            ),
            attestation.pointer.stateRef
        )
        return MembershipAttestation(
            attestation.attestor,
            membershipState,
            attestation.status,
            attestation.metadata,
            attestation.linearId,
            attestation.previousStateRef
        )
    }

    private fun getMembershipLinearId(): UniqueIdentifier {
        // The following expression is safe, since Membership is a LinearState.
        // For more info see the implementation of: StateAndRef<T>.toAttestationPointer()
        return attestation.pointer.stateLinearId!!
    }

    companion object {
        /**
         * These constants are used to create a temporary [StateAndRef] of [Membership], that is needed to satisfy
         * the public constructor of [MembershipAttestation]. These values will be discarded by that constructor.
         */
        const val BOGUS_CONTRACT_NAME = "BogusContract"
        const val BOGUS_NOTARY_NAME = "O=Org,L=New York,C=US"
    }
}
