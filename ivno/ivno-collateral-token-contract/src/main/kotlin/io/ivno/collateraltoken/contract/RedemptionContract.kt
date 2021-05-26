package io.ivno.collateraltoken.contract

import io.dasl.contracts.v1.token.TokenState
import io.onixlabs.corda.bnms.contract.membership.Membership
import io.onixlabs.corda.bnms.contract.membership.MembershipAttestation
import io.onixlabs.corda.identityframework.contract.AttestationStatus
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import java.math.BigDecimal
import java.security.PublicKey

class RedemptionContract : Contract {

    companion object {
        @JvmStatic
        val ID: ContractClassName = this::class.java.enclosingClass.canonicalName
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<RedemptionContractCommand>()
        when (command.value) {
            is Request, is Advance -> command.value.verify(tx, command.signers.toSet())
            else -> throw IllegalArgumentException("Unrecognised command: ${command.value}.")
        }
    }

    interface RedemptionContractCommand : CommandData {
        fun verify(tx: LedgerTransaction, signers: Set<PublicKey>)
    }

    object Request : RedemptionContractCommand {

        internal const val CONTRACT_RULE_REDEMPTION_INPUTS =
            "On redemption requesting, zero redemption states must be consumed."

        internal const val CONTRACT_RULE_REDEMPTION_OUTPUTS =
            "On redemption requesting, only one redemption state must be created."

        internal const val CONTRACT_RULE_TOKEN_INPUTS =
            "On redemption requesting, at least one token state must be consumed."

        internal const val CONTRACT_RULE_TOKEN_TYPE_REFERENCES =
            "On redemption requesting, only one Ivno token type must be referenced."

        internal const val CONTRACT_RULE_MEMBERSHIP_REFERENCES =
            "On redemption requesting, a membership state must be referenced for each redemption participant."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES =
            "On redemption requesting, a membership attestation state must be referenced for each redemption participant."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATION_STATUS =
            "On redemption requesting, every membership attestation status must be ACCEPTED."

        internal const val CONTRACT_RULE_MEMBERSHIP_NETWORK =
            "On redemption requesting, every membership's network must be equal to the Ivno token type network."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK =
            "On redemption requesting, every membership attestation's network must be equal to the Ivno token type network."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATIONS_POINT_TO_MEMBERSHIP_REFERENCES =
            "On redemption requesting, every membership attestation state must point to a referenced membership state."

        internal const val CONTRACT_RULE_PARTICIPANTS =
            "On redemption requesting, the redeemer, custodian and token issuing entity must be different."

        internal const val CONTRACT_RULE_AMOUNT =
            "On redemption requesting, the redemption amount must be equal to the sum of the input token amount."

        internal const val CONTRACT_RULE_SIGNERS =
            "On redemption requesting, all participants must sign the transaction."

        override fun verify(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
            val redemptionInputs = tx.inputsOfType<Redemption>()
            val redemptionOutputs = tx.outputsOfType<Redemption>()
            val tokenInputs = tx.inputsOfType<TokenState>()
            val tokenOutputs = tx.outputsOfType<TokenState>()
            val tokenTypeReferences = tx.referenceInputsOfType<IvnoTokenType>()
            val membershipReferences = tx.referenceInputRefsOfType<Membership>()
            val membershipAttestationReferences = tx.referenceInputsOfType<MembershipAttestation>()

            CONTRACT_RULE_REDEMPTION_INPUTS using (redemptionInputs.isEmpty())
            CONTRACT_RULE_REDEMPTION_OUTPUTS using (redemptionOutputs.size == 1)
            CONTRACT_RULE_TOKEN_INPUTS using (tokenInputs.isNotEmpty())
            CONTRACT_RULE_TOKEN_TYPE_REFERENCES using (tokenTypeReferences.size == 1)

            val redemptionOutput = redemptionOutputs.single()
            val tokenTypeReference = tokenTypeReferences.single()
            val membershipHolders = membershipReferences.map { it.state.data.holder }
            val membershipAttestationHolders = membershipAttestationReferences.map { it.holder }

            CONTRACT_RULE_MEMBERSHIP_REFERENCES using (redemptionOutput.participants.all { it in membershipHolders })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES using (redemptionOutput.participants.all { it in membershipAttestationHolders })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATION_STATUS using (membershipAttestationReferences.all { it.status == AttestationStatus.ACCEPTED })
            CONTRACT_RULE_MEMBERSHIP_NETWORK using (membershipReferences.all { it.state.data.network == tokenTypeReference.network })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK using (membershipAttestationReferences.all { it.network == tokenTypeReference.network })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATIONS_POINT_TO_MEMBERSHIP_REFERENCES using (redemptionOutput.participants.all { participant ->
                val membership = membershipReferences.single { it.state.data.holder == participant }
                val attestation = membershipAttestationReferences.single { it.holder == participant }

                attestation.pointer.isPointingTo(membership)
            })

            CONTRACT_RULE_PARTICIPANTS using (redemptionOutput.participants.isDistinct())

            val sumOfTokenInputs = tokenInputs
                .filter { it.owner == redemptionOutput.redeemer }
                .map { it.amount.quantity }
                .fold(BigDecimal.ZERO, BigDecimal::add)

            val sumOfTokenOutputs = tokenOutputs
                .filter { it.owner == redemptionOutput.redeemer }
                .map { it.amount.quantity }
                .fold(BigDecimal.ZERO, BigDecimal::add)

            val sumOfTokensToRedeem = sumOfTokenInputs - sumOfTokenOutputs

            CONTRACT_RULE_AMOUNT using (redemptionOutput.amount.quantity.compareTo(sumOfTokensToRedeem) == 0)
            CONTRACT_RULE_SIGNERS using (redemptionOutput.getRequiredSigningKeys().all { it in signers })
        }
    }

    object Advance : RedemptionContractCommand {

        internal const val CONTRACT_RULE_REDEMPTION_INPUTS =
            "On redemption advancing, only one redemption state must be consumed."

        internal const val CONTRACT_RULE_REDEMPTION_OUTPUTS =
            "On redemption advancing, only one redemption state must be created."

        internal const val CONTRACT_RULE_TOKEN_OUTPUTS =
            "On redemption advancing, only one token state must be created when the advance status is REJECTED."

        internal const val CONTRACT_RULE_TOKEN_TYPE_REFERENCES =
            "On redemption advancing, only one Ivno token type must be referenced."

        internal const val CONTRACT_RULE_MEMBERSHIP_REFERENCES =
            "On redemption advancing, a membership state must be referenced for each redemption participant."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES =
            "On redemption advancing, a membership attestation state must be referenced for each redemption participant."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATION_STATUS =
            "On redemption advancing, every membership attestation status must be ACCEPTED."

        internal const val CONTRACT_RULE_MEMBERSHIP_NETWORK =
            "On redemption advancing, every membership's network must be equal to the Ivno token type network."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK =
            "On redemption advancing, every membership attestation's network must be equal to the Ivno token type network."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATIONS_POINT_TO_MEMBERSHIP_REFERENCES =
            "On redemption advancing, every membership attestation state must point to a referenced membership state."

        internal const val CONTRACT_RULE_CHANGES =
            "On redemption advancing, the redeemer, custodian, token issuing entity, amount and linearId must not change."

        internal const val CONTRACT_RULE_CAN_ADVANCE =
            "On redemption advancing, the output state must be able to advance from the input state."

        internal const val CONTRACT_RULE_TOKEN_AMOUNT =
            "On redemption advancing, the token output amount must be equal to the redemption amount when the advance status is REJECTED."

        internal const val CONTRACT_RULE_TOKEN_HOLDER =
            "On redemption advancing, the token output holder must be equal to the redemption redeemer when the advance status is REJECTED."

        internal const val CONTRACT_RULE_SIGNERS =
            "On redemption advancing, the required signing participants must sign the transaction."

        override fun verify(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
            val redemptionInputs = tx.inputsOfType<Redemption>()
            val redemptionOutputs = tx.outputsOfType<Redemption>()
            val tokenOutputs = tx.outputsOfType<TokenState>()
            val tokenTypeReferences = tx.referenceInputsOfType<IvnoTokenType>()
            val membershipReferences = tx.referenceInputRefsOfType<Membership>()
            val membershipAttestationReferences = tx.referenceInputsOfType<MembershipAttestation>()

            CONTRACT_RULE_REDEMPTION_INPUTS using (redemptionInputs.size == 1)
            CONTRACT_RULE_REDEMPTION_OUTPUTS using (redemptionOutputs.size == 1)
            CONTRACT_RULE_TOKEN_TYPE_REFERENCES using (tokenTypeReferences.size == 1)

            val redemptionInput = redemptionInputs.single()
            val redemptionOutput = redemptionOutputs.single()
            val tokenTypeReference = tokenTypeReferences.single()
            val membershipHolders = membershipReferences.map { it.state.data.holder }
            val membershipAttestationHolders = membershipAttestationReferences.map { it.holder }

            CONTRACT_RULE_MEMBERSHIP_REFERENCES using (redemptionOutput.participants.all { it in membershipHolders })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES using (redemptionOutput.participants.all { it in membershipAttestationHolders })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATION_STATUS using (membershipAttestationReferences.all { it.status == AttestationStatus.ACCEPTED })
            CONTRACT_RULE_MEMBERSHIP_NETWORK using (membershipReferences.all { it.state.data.network == tokenTypeReference.network })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK using (membershipAttestationReferences.all { it.network == tokenTypeReference.network })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATIONS_POINT_TO_MEMBERSHIP_REFERENCES using (redemptionOutput.participants.all { participant ->
                val membership = membershipReferences.single { it.state.data.holder == participant }
                val attestation = membershipAttestationReferences.single { it.holder == participant }

                attestation.pointer.isPointingTo(membership)
            })

            CONTRACT_RULE_CHANGES using (redemptionInput.immutableEquals(redemptionOutput))
            CONTRACT_RULE_CAN_ADVANCE using (redemptionOutput.status.canAdvanceFrom(redemptionInput.status))

            if (redemptionOutput.status == RedemptionStatus.REJECTED) {
                CONTRACT_RULE_TOKEN_OUTPUTS using (tokenOutputs.size == 1)

                val tokenOutput = tokenOutputs.single()

                CONTRACT_RULE_TOKEN_AMOUNT using (tokenOutput.amount.quantity.compareTo(redemptionOutput.amount.quantity) == 0)
                CONTRACT_RULE_TOKEN_HOLDER using (tokenOutput.owner == redemptionOutput.redeemer)
                CONTRACT_RULE_SIGNERS using (redemptionOutput.getRequiredSigningKeys().all { it in signers })
            }
        }
    }
}
