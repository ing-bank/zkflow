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
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import java.io.File
import java.math.BigDecimal
import java.security.PublicKey

class TransferContract : Contract {

    companion object {
        @JvmStatic
        val ID: ContractClassName = this::class.java.enclosingClass.canonicalName
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<TransferContractCommand>()
        when (command.value) {
            is Request, is Advance -> command.value.verify(tx, command.signers.toSet())
            else -> throw IllegalArgumentException("Unrecognised command: ${command.value}.")
        }
    }

    interface TransferContractCommand : ZKCommandData {
        fun verify(tx: LedgerTransaction, signers: Set<PublicKey>)
    }

    @Serializable
    object Request : TransferContractCommand {
        @Transient
        override val circuit: CircuitMetaData = CircuitMetaData.fromConfig(
            File("${System.getProperty("user.dir")}/build/zinc/transfer-request")
        )

        internal const val CONTRACT_RULE_TRANSFER_INPUTS =
            "On transfer requesting, zero transfer states must be consumed."

        internal const val CONTRACT_RULE_TRANSFER_OUTPUTS =
            "On transfer requesting, only one transfer state must be created."

        internal const val CONTRACT_RULE_TOKEN_TYPE_REFERENCES =
            "On transfer requesting, only one Ivno token type must be referenced."

        internal const val CONTRACT_RULE_MEMBERSHIP_REFERENCES =
            "On transfer requesting, a membership state must be referenced for each transfer participant."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES =
            "On transfer requesting, a membership attestation state must be referenced for each transfer participant."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATION_STATUS =
            "On transfer requesting, every membership attestation status must be ACCEPTED."

        internal const val CONTRACT_RULE_MEMBERSHIP_NETWORK =
            "On transfer requesting, every membership's network must be equal to the Ivno token type network."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK =
            "On transfer requesting, every membership attestation's network must be equal to the Ivno token type network."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATIONS_POINT_TO_MEMBERSHIP_REFERENCES =
            "On transfer requesting, every membership attestation state must point to a referenced membership state."

        internal const val CONTRACT_RULE_PARTICIPANTS =
            "On transfer requesting, the sender and the receiver accounts must not be the same."

        internal const val CONTRACT_RULE_AMOUNT =
            "On transfer requesting, the amount must be greater than zero."

        internal const val CONTRACT_RULE_STATUS =
            "On transfer requesting, the status must be REQUESTED."

        internal const val CONTRACT_RULE_SIGNERS =
            "On transfer requesting, the initiator must sign the transaction."

        override fun verify(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
            val transferInputs = tx.inputsOfType<Transfer>()
            val transferOutputs = tx.outputsOfType<Transfer>()
            val tokenTypeReferences = tx.referenceInputsOfType<IvnoTokenType>()
            val membershipReferences = tx.referenceInputRefsOfType<Membership>()
            val membershipAttestationReferences = tx.referenceInputsOfType<MembershipAttestation>()

            CONTRACT_RULE_TRANSFER_INPUTS using (transferInputs.isEmpty())
            CONTRACT_RULE_TRANSFER_OUTPUTS using (transferOutputs.size == 1)
            CONTRACT_RULE_TOKEN_TYPE_REFERENCES using (tokenTypeReferences.size == 1)

            val transferOutput = transferOutputs.single()
            val tokenTypeReference = tokenTypeReferences.single()
            val membershipHolders = membershipReferences.map { it.state.data.holder }
            val membershipAttestationHolders = membershipAttestationReferences.map { it.holder }

            CONTRACT_RULE_MEMBERSHIP_REFERENCES using (transferOutput.participants.all { it in membershipHolders })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES using (transferOutput.participants.all { it in membershipAttestationHolders })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATION_STATUS using (membershipAttestationReferences.all { it.status == AttestationStatus.ACCEPTED })
            CONTRACT_RULE_MEMBERSHIP_NETWORK using (membershipReferences.all { it.state.data.network == tokenTypeReference.network })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK using (membershipAttestationReferences.all { it.network == tokenTypeReference.network })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATIONS_POINT_TO_MEMBERSHIP_REFERENCES using (transferOutput.participants.all { participant ->
                val membership = membershipReferences.single { it.state.data.holder == participant }
                val attestation = membershipAttestationReferences.single { it.holder == participant }

                attestation.pointer.isPointingTo(membership)
            })

            CONTRACT_RULE_PARTICIPANTS using (transferOutput.currentTokenHolderAccountId != transferOutput.targetTokenHolderAccountId ||
                transferOutput.currentTokenHolder != transferOutput.targetTokenHolder)
            CONTRACT_RULE_AMOUNT using (transferOutput.amount > BigDecimalAmount(BigDecimal.ZERO, transferOutput.amount.amountType))
            CONTRACT_RULE_STATUS using (transferOutput.status == TransferStatus.REQUESTED)
            CONTRACT_RULE_SIGNERS using (transferOutput.getRequiredSigningKey() in signers)
        }
    }

    object Advance : TransferContractCommand {
        @Transient
        override val circuit: CircuitMetaData = CircuitMetaData.fromConfig(
            File("${System.getProperty("user.dir")}/build/zinc/transfer-advance")
        )


        internal const val CONTRACT_RULE_TRANSFER_INPUTS =
            "On transfer advancing, only one transfer state must be consumed."

        internal const val CONTRACT_RULE_TRANSFER_OUTPUTS =
            "On transfer advancing, only one transfer state must be created."

        internal const val CONTRACT_RULE_TOKEN_INPUTS =
            "On transfer advancing, at least one token must be consumed when the advance status is COMPLETED."

        internal const val CONTRACT_RULE_TOKEN_OUTPUTS =
            "On transfer advancing, at least one token must be created when the advance status is COMPLETED."

        internal const val CONTRACT_RULE_TOKEN_TYPE_REFERENCES =
            "On transfer advancing, only one Ivno token type must be referenced."

        internal const val CONTRACT_RULE_MEMBERSHIP_REFERENCES =
            "On transfer advancing, a membership state must be referenced for each transfer participant."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES =
            "On transfer advancing, a membership attestation state must be referenced for each transfer participant."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATION_STATUS =
            "On transfer advancing, every membership attestation status must be ACCEPTED."

        internal const val CONTRACT_RULE_MEMBERSHIP_NETWORK =
            "On transfer advancing, every membership's network must be equal to the Ivno token type network."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK =
            "On transfer advancing, every membership attestation's network must be equal to the Ivno token type network."

        internal const val CONTRACT_RULE_MEMBERSHIP_ATTESTATIONS_POINT_TO_MEMBERSHIP_REFERENCES =
            "On transfer advancing, every membership attestation state must point to a referenced membership state."

        internal const val CONTRACT_RULE_CHANGES =
            "On transfer advancing, the sender, receiver, initiator, amount and linearId must not change."

        internal const val CONTRACT_RULE_CAN_ADVANCE =
            "On transfer advancing, the output state must be able to advance from the input state."

        internal const val CONTRACT_RULE_TOKEN_AMOUNT =
            "On transfer advancing, the transaction must include a token output of equal value to the transfer amount."

        internal const val CONTRACT_RULE_TIMESTAMP =
            "On transfer advancing, the created timestamp must occur after the consumed timestamp."

        internal const val CONTRACT_RULE_SIGNERS =
            "On transfer advancing, the advancing participant must sign the transaction."

        override fun verify(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
            val transferInputs = tx.inputsOfType<Transfer>()
            val tokenInputs = tx.inputsOfType<TokenState>()
            val transferOutputs = tx.outputsOfType<Transfer>()
            val tokenOutputs = tx.outputsOfType<TokenState>()
            val tokenTypeReferences = tx.referenceInputsOfType<IvnoTokenType>()
            val membershipReferences = tx.referenceInputRefsOfType<Membership>()
            val membershipAttestationReferences = tx.referenceInputsOfType<MembershipAttestation>()

            CONTRACT_RULE_TRANSFER_INPUTS using (transferInputs.size == 1)
            CONTRACT_RULE_TRANSFER_OUTPUTS using (transferOutputs.size == 1)
            CONTRACT_RULE_TOKEN_TYPE_REFERENCES using (tokenTypeReferences.size == 1)

            val transferInput = transferInputs.single()
            val transferOutput = transferOutputs.single()
            val tokenTypeReference = tokenTypeReferences.single()
            val membershipHolders = membershipReferences.map { it.state.data.holder }
            val membershipAttestationHolders = membershipAttestationReferences.map { it.holder }

            CONTRACT_RULE_MEMBERSHIP_REFERENCES using (transferOutput.participants.all { it in membershipHolders })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES using (transferOutput.participants.all { it in membershipAttestationHolders })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATION_STATUS using (membershipAttestationReferences.all { it.status == AttestationStatus.ACCEPTED })
            CONTRACT_RULE_MEMBERSHIP_NETWORK using (membershipReferences.all { it.state.data.network == tokenTypeReference.network })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK using (membershipAttestationReferences.all { it.network == tokenTypeReference.network })
            CONTRACT_RULE_MEMBERSHIP_ATTESTATIONS_POINT_TO_MEMBERSHIP_REFERENCES using (transferOutput.participants.all { participant ->
                val membership = membershipReferences.single { it.state.data.holder == participant }
                val attestation = membershipAttestationReferences.single { it.holder == participant }

                attestation.pointer.isPointingTo(membership)
            })

            CONTRACT_RULE_CHANGES using (transferInput.immutableEquals(transferOutput))
            CONTRACT_RULE_CAN_ADVANCE using (transferOutput.status.canAdvanceFrom(transferInput.status))

            if (transferOutput.status == TransferStatus.COMPLETED) {
                CONTRACT_RULE_TOKEN_INPUTS using (tokenInputs.isNotEmpty())
                CONTRACT_RULE_TOKEN_OUTPUTS using (tokenOutputs.isNotEmpty())
                CONTRACT_RULE_TOKEN_AMOUNT using (tokenOutputs.any {
                    it.amount.quantity.compareTo(transferOutput.amount.quantity) == 0
                })
            }

            CONTRACT_RULE_TIMESTAMP using (transferOutput.timestamp > transferInput.timestamp)
            CONTRACT_RULE_SIGNERS using (transferOutput.getRequiredSigningKey() in signers)
        }
    }
}
