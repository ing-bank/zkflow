package io.ivno.collateraltoken.contract

import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.zkp.CircuitMetaData
import io.dasl.contracts.v1.token.BigDecimalAmount
import io.dasl.contracts.v1.token.TokenState
import io.onixlabs.corda.bnms.contract.membership.Membership
import io.onixlabs.corda.bnms.contract.membership.MembershipAttestation
import io.onixlabs.corda.identityframework.contract.AttestationStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.io.File
import java.math.BigDecimal
import java.security.PublicKey
import net.corda.core.contracts.ComponentGroupEnum


class DepositContract : Contract {

    companion object {
        @JvmStatic
        val ID: ContractClassName = this::class.java.enclosingClass.canonicalName
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<DepositContractCommand>()
        when (command.value) {
            is Request, is Advance -> command.value.verify(tx, command.signers.toSet())
            else -> throw IllegalArgumentException("Unrecognised command: ${command.value}.")
        }
    }

    interface DepositContractCommand : ZKCommandData {
        fun verify(tx: LedgerTransaction, signers: Set<PublicKey>)
    }

    @Serializable
    object Request : DepositContractCommand {
        internal const val CONTRACT_RULE_DEPOSIT_INPUTS =
            "On deposit requesting, zero deposit states must be consumed."

        internal const val CONTRACT_RULE_DEPOSIT_OUTPUTS =
            "On deposit requesting, only one deposit state must be created."

        internal const val CONTRACT_RULE_TOKEN_TYPE_REFERENCES =
            "On deposit requesting, only one Ivno token type must be referenced."

        internal const val CONTRACT_RULE_MEMBERSHIP_REFERENCES =
            "On deposit requesting, a membership state must be referenced for each deposit participant."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES =
            "On deposit requesting, a membership attestation state must be referenced for each deposit participant."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATION_STATUS =
            "On deposit requesting, every membership attestation status must be ACCEPTED."

        internal const val CONTRACT_RULE_MEMBERSHIP_NETWORK =
            "On deposit requesting, every membership's network must be equal to the Ivno token type network."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK =
            "On deposit requesting, every membership attestation's network must be equal to the Ivno token type network."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATIONS_POINT_TO_MEMBERSHIP_REFERENCES =
            "On deposit requesting, every membership attestation state must point to a referenced membership state."

        internal const val CONTRACT_RULE_PARTICIPANTS =
            "On deposit requesting, the depositor, custodian and token issuing entity must be different participants."

        internal const val CONTRACT_RULE_AMOUNT =
            "On deposit requesting, the amount must be greater than zero."

        internal const val CONTRACT_RULE_REFERENCE =
            "On deposit requesting, the reference must be null."

        internal const val CONTRACT_RULE_STATUS =
            "On deposit requesting, the status must be DEPOSIT_REQUESTED."

        internal const val CONTRACT_RULE_SIGNERS =
            "On deposit requesting, the depositor must sign the transaction."

        override fun verify(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
            val depositInputs = tx.inputsOfType<Deposit>()
            val depositOutputs = tx.outputsOfType<Deposit>()
            val tokenTypeReferences = tx.referenceInputsOfType<IvnoTokenType>()
            val membershipReferences = tx.referenceInputRefsOfType<Membership>()
            val membershipAttestationReferences = tx.referenceInputsOfType<MembershipAttestation>()

            CONTRACT_RULE_DEPOSIT_INPUTS using (depositInputs.isEmpty())
            CONTRACT_RULE_DEPOSIT_OUTPUTS using (depositOutputs.size == 1)
            CONTRACT_RULE_TOKEN_TYPE_REFERENCES using (tokenTypeReferences.size == 1)

            val depositOutput = depositOutputs.single()
            val tokenTypeReference = tokenTypeReferences.single()
            val membershipHolders = membershipReferences.map { it.state.data.holder }
            val membershipAttestationHolders = membershipAttestationReferences.map { it.holder }

            CONTRACT_RULE_MEMBERSHIP_REFERENCES using (depositOutput.participants.all { it in membershipHolders })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES using (depositOutput.participants.all { it in membershipAttestationHolders })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATION_STATUS using (membershipAttestationReferences.all { it.status == AttestationStatus.ACCEPTED })
            CONTRACT_RULE_MEMBERSHIP_NETWORK using (membershipReferences.all { it.state.data.network == tokenTypeReference.network })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK using (membershipAttestationReferences.all { it.network == tokenTypeReference.network })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATIONS_POINT_TO_MEMBERSHIP_REFERENCES using (depositOutput.participants.all { participant ->
                val membership = membershipReferences.single { it.state.data.holder == participant }
                val attestation = membershipAttestationReferences.single { it.holder == participant }

                attestation.pointer.isPointingTo(membership)
            })

            CONTRACT_RULE_PARTICIPANTS using (depositOutput.participants.isDistinct())
            CONTRACT_RULE_AMOUNT using (depositOutput.amount > BigDecimalAmount(
                BigDecimal.ZERO,
                depositOutput.amount.amountType
            ))
            CONTRACT_RULE_REFERENCE using (depositOutput.reference == null)
            CONTRACT_RULE_STATUS using (depositOutput.status == DepositStatus.DEPOSIT_REQUESTED)
            CONTRACT_RULE_SIGNERS using (depositOutput.getRequiredSigningKeys().all { it in signers })
        }

        @Transient
        override val circuit: CircuitMetaData = CircuitMetaData.Builder()
            .name("Request")
            .addComponentGroupSize(ComponentGroupEnum.SIGNERS_GROUP, 2)
            .build()
    }

    @Serializable
    object Advance : DepositContractCommand {

        internal const val CONTRACT_RULE_DEPOSIT_INPUTS =
            "On deposit advancing, only one deposit state must be consumed."

        internal const val CONTRACT_RULE_DEPOSIT_OUTPUTS =
            "On deposit advancing, only one deposit state must be created."

        internal const val CONTRACT_RULE_TOKEN_OUTPUTS =
            "On deposit advancing, only one token state must be created when the advance status is PAYMENT_ACCEPTED."

        internal const val CONTRACT_RULE_TOKEN_TYPE_REFERENCES =
            "On deposit advancing, only one Ivno token type must be referenced."

        internal const val CONTRACT_RULE_MEMBERSHIP_REFERENCES =
            "On deposit advancing, a membership state must be referenced for each deposit participant."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES =
            "On deposit advancing, a membership attestation state must be referenced for each deposit participant."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATION_STATUS =
            "On deposit advancing, every membership attestation status must be ACCEPTED."

        internal const val CONTRACT_RULE_MEMBERSHIP_NETWORK =
            "On deposit advancing, every membership's network must be equal to the Ivno token type network."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK =
            "On deposit advancing, every membership attestation's network must be equal to the Ivno token type network."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATIONS_POINT_TO_MEMBERSHIP_REFERENCES =
            "On deposit advancing, every membership attestation state must point to a referenced membership state."

        internal const val CONTRACT_RULE_CHANGES =
            "On deposit advancing, the depositor, custodian, amount and linearId must not change."

        internal const val CONTRACT_RULE_CAN_ADVANCE =
            "On deposit advancing, the output state must be able to advance from the input state."

        internal const val CONTRACT_RULE_REFERENCE =
            "On deposit advancing, the reference must only change if the advanced status is DEPOSIT_ACCEPTED, and must not be null."

        internal const val CONTRACT_RULE_TIMESTAMP =
            "On deposit advancing, the created timestamp must occur after the consumed timestamp."

        internal const val CONTRACT_RULE_TOKEN_ISSUING_ENTITY =
            "On deposit advancing, the referenced token type and the deposit token issuing entity must be equal."

        internal const val CONTRACT_RULE_TOKEN_AMOUNT =
            "On deposit advancing, the token amount must be of equal value to the transfer amount."

        internal const val CONTRACT_RULE_SIGNERS =
            "On deposit advancing, the required signing participants must sign the transaction."

        override fun verify(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
            val depositInputs = tx.inputsOfType<Deposit>()
            val depositOutputs = tx.outputsOfType<Deposit>()
            val tokenTypeReferences = tx.referenceInputsOfType<IvnoTokenType>()
            val membershipReferences = tx.referenceInputRefsOfType<Membership>()
            val membershipAttestationReferences = tx.referenceInputsOfType<MembershipAttestation>()

            CONTRACT_RULE_DEPOSIT_INPUTS using (depositInputs.size == 1)
            CONTRACT_RULE_DEPOSIT_OUTPUTS using (depositOutputs.size == 1)
            CONTRACT_RULE_TOKEN_TYPE_REFERENCES using (tokenTypeReferences.size == 1)

            val depositInput = depositInputs.single()
            val depositOutput = depositOutputs.single()
            val tokenTypeReference = tokenTypeReferences.single()
            val membershipHolders = membershipReferences.map { it.state.data.holder }
            val membershipAttestationHolders = membershipAttestationReferences.map { it.holder }

            CONTRACT_RULE_MEMBERSHIP_REFERENCES using (depositOutput.participants.all { it in membershipHolders })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES using (depositOutput.participants.all { it in membershipAttestationHolders })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATION_STATUS using (membershipAttestationReferences.all { it.status == AttestationStatus.ACCEPTED })
            CONTRACT_RULE_MEMBERSHIP_NETWORK using (membershipReferences.all { it.state.data.network == tokenTypeReference.network })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK using (membershipAttestationReferences.all { it.network == tokenTypeReference.network })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATIONS_POINT_TO_MEMBERSHIP_REFERENCES using (depositOutput.participants.all { participant ->
                val membership = membershipReferences.single { it.state.data.holder == participant }
                val attestation = membershipAttestationReferences.single { it.holder == participant }

                attestation.pointer.isPointingTo(membership)
            })

            CONTRACT_RULE_CHANGES using (depositInput.immutableEquals(depositOutput))
            CONTRACT_RULE_CAN_ADVANCE using (depositOutput.status.canAdvanceFrom(depositInput.status))

            CONTRACT_RULE_REFERENCE using when (depositOutput.status) {
                DepositStatus.DEPOSIT_ACCEPTED -> depositOutput.reference != null
                else -> depositInput.reference == depositOutput.reference
            }

            if (depositOutput.status == DepositStatus.PAYMENT_ACCEPTED) {
                val tokenOutputs = tx.outputsOfType<TokenState>()

                CONTRACT_RULE_TOKEN_OUTPUTS using (tokenOutputs.size == 1)

                val tokenOutput = tokenOutputs.single()

                CONTRACT_RULE_TOKEN_ISSUING_ENTITY using (depositOutput.tokenIssuingEntity.owningKey == tokenTypeReference.tokenIssuingEntity.owningKey)
                CONTRACT_RULE_TOKEN_AMOUNT using (tokenOutput.amount.quantity.compareTo(depositOutput.amount.quantity) == 0)
            }

            CONTRACT_RULE_TIMESTAMP using (depositOutput.timestamp > depositInput.timestamp)
            CONTRACT_RULE_SIGNERS using (depositOutput.getRequiredSigningKeys().all { it in signers })
        }

        @Transient
        override val circuit: CircuitMetaData = CircuitMetaData.Builder()
            .name("Advance")
            .addComponentGroupSize(ComponentGroupEnum.SIGNERS_GROUP, 2)
            .build()
    }
}
