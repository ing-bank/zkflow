package io.ivno.collateraltoken.contract

import io.dasl.contracts.v1.crud.CrudCommands
import io.dasl.contracts.v1.token.TokenTypeContract
import io.onixlabs.corda.bnms.contract.membership.Membership
import io.onixlabs.corda.bnms.contract.membership.MembershipAttestation
import io.onixlabs.corda.identityframework.contract.AttestationStatus
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

class IvnoTokenTypeContract : TokenTypeContract(), Contract {

    companion object {
        @JvmStatic
        val ID: ContractClassName = this::class.java.enclosingClass.canonicalName
    }

    override fun verify(tx: LedgerTransaction) {
        super.verify(tx)
        val command = tx.commands.requireSingleCommand<CrudCommands>()
        if (command.value is CrudCommands.Create) {
            additionalCreateChecks(tx, command.signers.toSet())
        }
    }

    internal object Create {
        const val CONTRACT_RULE_MEMBERSHIP_REFERENCE =
            "On Ivno token type creating, a membership state must be referenced for the token issuing entity."

        const val CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCE =
            "On Ivno token type creating, a membership attestation state must be referenced for the token issuing entity."

        const val CONTRACT_RULE_ATTESTATION_POINTS_TO_MEMBERSHIP =
            "On Ivno token type creating, the referenced membership attestation state must point to the referenced membership state."

        const val CONTRACT_RULE_MEMBERSHIP_ROLES =
            "On Ivno token type creating, the referenced membership state must possess the TOKEN_ISSUING_ENTITY role."

        const val CONTRACT_RULE_MEMBERSHIP_ATTESTATION_STATUS =
            "On Ivno token type creating, the membership attestation status must be ACCEPTED."

        const val CONTRACT_RULE_MEMBERSHIP_AND_TOKEN_TYPE_NETWORK =
            "On Ivno token type creating, the issued token type network must be equal to the membership network."

        const val CONTRACT_RULE_SIGNERS =
            "On Ivno token type creating, the token issuing entity must sign the transaction."
    }

    private fun additionalCreateChecks(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        val tokenTypeOutput = tx.outputsOfType<IvnoTokenType>().single()
        val membershipReferences = tx.referenceInputRefsOfType<Membership>()
        val membershipAttestationReferences = tx.referenceInputsOfType<MembershipAttestation>()

        Create.CONTRACT_RULE_MEMBERSHIP_REFERENCE using (membershipReferences.any {
            it.state.data.holder == tokenTypeOutput.tokenIssuingEntity
        })

        Create.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCE using (membershipAttestationReferences.any {
            it.holder == tokenTypeOutput.tokenIssuingEntity
        })

        val tieMembership = membershipReferences.single {
            it.state.data.holder == tokenTypeOutput.tokenIssuingEntity
        }

        val tieMembershipAttestation = membershipAttestationReferences.single {
            it.holder == tokenTypeOutput.tokenIssuingEntity
        }

        Create.CONTRACT_RULE_ATTESTATION_POINTS_TO_MEMBERSHIP using
                (tieMembershipAttestation.pointer.isPointingTo(tieMembership))

        Create.CONTRACT_RULE_MEMBERSHIP_ROLES using
                (tieMembership.state.data.hasRole("TOKEN_ISSUING_ENTITY"))

        Create.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_STATUS using
                (tieMembershipAttestation.status == AttestationStatus.ACCEPTED)

        Create.CONTRACT_RULE_MEMBERSHIP_AND_TOKEN_TYPE_NETWORK using (tokenTypeOutput.network == tieMembership.state.data.network)

        Create.CONTRACT_RULE_SIGNERS using (tokenTypeOutput.tokenIssuingEntity.owningKey in signers)
    }
}
