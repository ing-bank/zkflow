package io.ivno.collateraltoken.contract

import com.ing.zknotary.testing.dsl.VerificationMode
import com.ing.zknotary.testing.dsl.zkLedger
import com.ing.zknotary.testing.zkp.MockZKTransactionService
import io.dasl.contracts.v1.token.TokenContract
import io.onixlabs.corda.bnms.contract.Network
import io.onixlabs.corda.identityframework.contract.AttestationStatus
import net.corda.core.contracts.UniqueIdentifier
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Disabled("Re-enable once we have everything serializable and when we have zktransaction DSL")
class TransferContractAdvanceTests : ContractTest() {
    override val verificationMode = VerificationMode.PROVE_AND_VERIFY
    override val commandData = TransferContract.Advance

    @Test
    fun `On transfer advancing, the transaction must include the Advance command`() {
        services.zkLedger(zkService = MockZKTransactionService(services)) {
        // services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(BANK_B).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(BANK_B).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                input(TransferContract.ID, TRANSFER)
                output(TransferContract.ID, TRANSFER.acceptTransfer())
                fails()
                command(keysOf(BANK_B), commandData)
                verifies(verificationMode)
            }
        }
    }

    @Test
    fun `On transfer advancing, only one transfer state must be consumed`() {
        services.zkLedger {
            zkTransaction {
                input(TransferContract.ID, TRANSFER)
                input(TransferContract.ID, TRANSFER)
                output(TransferContract.ID, TRANSFER.acceptTransfer())
                command(keysOf(BANK_B), commandData)
                failsWith(commandData.CONTRACT_RULE_TRANSFER_INPUTS)
            }
        }
    }

    @Test
    fun `On transfer advancing, only one transfer state must be created`() {
        services.zkLedger {
            zkTransaction {
                input(TransferContract.ID, TRANSFER)
                output(TransferContract.ID, TRANSFER.acceptTransfer())
                output(TransferContract.ID, TRANSFER.acceptTransfer())
                command(keysOf(BANK_B), commandData)
                failsWith(commandData.CONTRACT_RULE_TRANSFER_OUTPUTS)
            }
        }
    }

    @Test
    fun `On transfer advancing, at least one token state must be consumed when the advance status is COMPLETED`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(BANK_B).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(BANK_B).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                input(TransferContract.ID, TRANSFER.acceptTransfer())
                output(TransferContract.ID, TRANSFER.acceptTransfer().completeTransfer())
                output(TokenContract.CONTRACT_ID, TOKEN_100GBP_BANK_B)
                command(keysOf(BANK_B), commandData)
                failsWith(commandData.CONTRACT_RULE_TOKEN_INPUTS)
            }
        }
    }

    @Test
    fun `On transfer advancing, at least one token state must be created when the advance status is COMPLETED`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(BANK_B).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(BANK_B).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                input(TransferContract.ID, TRANSFER.acceptTransfer())
                input(TokenContract.CONTRACT_ID, TOKEN_100GBP_BANK_A)
                output(TransferContract.ID, TRANSFER.acceptTransfer().completeTransfer())
                command(keysOf(BANK_B), commandData)
                failsWith(commandData.CONTRACT_RULE_TOKEN_OUTPUTS)
            }
        }
    }

    @Test
    fun `On transfer advancing, only one Ivno token type must be referenced`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(BANK_B).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(BANK_B).ref)
                input(TransferContract.ID, TRANSFER)
                output(TransferContract.ID, TRANSFER.acceptTransfer())
                command(keysOf(BANK_B), commandData)
                failsWith(commandData.CONTRACT_RULE_TOKEN_TYPE_REFERENCES)
            }
        }
    }

    @Test
    fun `On transfer advancing, a membership state must be referenced for each transfer participant (BANK_A missing)`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_B).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(BANK_B).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                input(TransferContract.ID, TRANSFER)
                output(TransferContract.ID, TRANSFER.acceptTransfer())
                command(keysOf(BANK_B), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_REFERENCES)
            }
        }
    }

    @Test
    fun `On transfer advancing, a membership state must be referenced for each transfer participant (BANK_B missing)`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(BANK_B).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                input(TransferContract.ID, TRANSFER)
                output(TransferContract.ID, TRANSFER.acceptTransfer())
                command(keysOf(BANK_B), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_REFERENCES)
            }
        }
    }

    @Test
    fun `On transfer advancing, a membership attestation state must be referenced for each transfer participant (BANK_A missing)`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(BANK_B).ref)
                reference(memberships.attestationFor(BANK_B).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                input(TransferContract.ID, TRANSFER)
                output(TransferContract.ID, TRANSFER.acceptTransfer())
                command(keysOf(BANK_B), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES)
            }
        }
    }

    @Test
    fun `On transfer advancing, a membership attestation state must be referenced for each transfer participant (BANK_B missing)`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(BANK_B).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                input(TransferContract.ID, TRANSFER)
                output(TransferContract.ID, TRANSFER.acceptTransfer())
                command(keysOf(BANK_B), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES)
            }
        }
    }

    @Test
    fun `On transfer advancing, every membership attestation status must be ACCEPTED`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships(status = AttestationStatus.REJECTED)
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(BANK_B).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(BANK_B).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                input(TransferContract.ID, TRANSFER)
                output(TransferContract.ID, TRANSFER.acceptTransfer())
                command(keysOf(BANK_B), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_STATUS)
            }
        }
    }

    @Test
    fun `On transfer advancing, every membership's network must be equal to the Ivno token type network (BANK_A invalid)`() {
        services.zkLedger {
            zkTransaction {
                val network = Network("INVALID_NETWORK", BNO.party)
                val memberships = createAllMemberships()
                val (invalidMembership, _) = createMembership(BANK_A.party, network = network)
                reference(invalidMembership.ref)
                reference(memberships.membershipFor(BANK_B).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(BANK_B).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                input(TransferContract.ID, TRANSFER)
                output(TransferContract.ID, TRANSFER.acceptTransfer())
                command(keysOf(BANK_B), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_NETWORK)
            }
        }
    }

    @Test
    fun `On transfer advancing, every membership's network must be equal to the Ivno token type network (BANK_B invalid)`() {
        services.zkLedger {
            zkTransaction {
                val network = Network("INVALID_NETWORK", BNO.party)
                val memberships = createAllMemberships()
                val (invalidMembership, _) = createMembership(BANK_B.party, network = network)
                reference(memberships.membershipFor(BANK_A).ref)
                reference(invalidMembership.ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(BANK_B).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                input(TransferContract.ID, TRANSFER)
                output(TransferContract.ID, TRANSFER.acceptTransfer())
                command(keysOf(BANK_B), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_NETWORK)
            }
        }
    }

    @Test
    fun `On transfer advancing, every membership attestation's network must be equal to the Ivno token type network (BANK_A invalid)`() {
        services.zkLedger {
            zkTransaction {
                val network = Network("INVALID_NETWORK", BNO.party)
                val memberships = createAllMemberships()
                val (_, invalidAttestation) = createMembership(BANK_A.party, network = network)
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(BANK_B).ref)
                reference(invalidAttestation.ref)
                reference(memberships.attestationFor(BANK_B).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                input(TransferContract.ID, TRANSFER)
                output(TransferContract.ID, TRANSFER.acceptTransfer())
                command(keysOf(BANK_B), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK)
            }
        }
    }

    @Test
    fun `On transfer advancing, every membership attestation's network must be equal to the Ivno token type network (BANK_B invalid)`() {
        services.zkLedger {
            zkTransaction {
                val network = Network("INVALID_NETWORK", BNO.party)
                val memberships = createAllMemberships()
                val (_, invalidAttestation) = createMembership(BANK_B.party, network = network)
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(BANK_B).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(invalidAttestation.ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                input(TransferContract.ID, TRANSFER)
                output(TransferContract.ID, TRANSFER.acceptTransfer())
                command(keysOf(BANK_B), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK)
            }
        }
    }

    @Test
    fun `On transfer advancing, every membership attestation state must point to a referenced membership state`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                val (bankMembership, bankAttestation) = createMembership(BANK_A.party, evolveMembership = true)
                reference(bankMembership.ref)
                reference(memberships.membershipFor(BANK_B).ref)
                reference(bankAttestation.ref)
                reference(memberships.attestationFor(BANK_B).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                input(TransferContract.ID, TRANSFER)
                output(TransferContract.ID, TRANSFER.acceptTransfer())
                command(keysOf(BANK_B), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_ATTESTATIONS_POINT_TO_MEMBERSHIP_REFERENCES)
            }
        }
    }

    @Test
    fun `On transfer advancing, the sender, receiver, initiator, amount and linearId must not change`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(BANK_B).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(BANK_B).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                input(TransferContract.ID, TRANSFER)
                output(TransferContract.ID, TRANSFER.acceptTransfer().copy(linearId = UniqueIdentifier()))
                command(keysOf(BANK_B), commandData)
                failsWith(commandData.CONTRACT_RULE_CHANGES)
            }
        }
    }

    @Test
    fun `On transfer advancing, the output state must be able to advance from the input state`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(BANK_B).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(BANK_B).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                input(TransferContract.ID, TRANSFER.cancelTransfer())
                output(TransferContract.ID, TRANSFER.acceptTransfer())
                command(keysOf(BANK_B), commandData)
                failsWith(commandData.CONTRACT_RULE_CAN_ADVANCE)
            }
        }
    }

    @Test
    fun `On transfer advancing, the transaction must include a token output of equal value to the transfer amount`() {
        services.zkLedger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            val requestedTransfer = requestTransfer(TRANSFER, tokenType)
            val acceptedTransfer = acceptTransfer(requestedTransfer, tokenType)

            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(BANK_B).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(BANK_B).ref)
                reference(tokenType.ref)
                input(token.ref)
                input(acceptedTransfer.ref)
                output(TokenContract.CONTRACT_ID, TOKEN_50GBP_BANK_A)
                output(TokenContract.CONTRACT_ID, TOKEN_50GBP_BANK_B)
                output(TransferContract.ID, TRANSFER.completeTransfer())
                command(keysOf(BANK_A), commandData)
                command(keysOf(BANK_A), TokenContract.Command.Move())
                failsWith(commandData.CONTRACT_RULE_TOKEN_AMOUNT)
            }
        }
    }

    @Test
    fun `On transfer advancing, the transaction must include a token output of equal value to the transfer amount (verifies)`() {
        services.zkLedger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            val requestedTransfer = requestTransfer(TRANSFER, tokenType)
            val acceptedTransfer = acceptTransfer(requestedTransfer, tokenType)

            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(BANK_B).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(BANK_B).ref)
                reference(tokenType.ref)
                input(token.ref)
                input(acceptedTransfer.ref)
                output(TokenContract.CONTRACT_ID, TOKEN_100GBP_BANK_B)
                output(TransferContract.ID, TRANSFER.completeTransfer())
                command(keysOf(BANK_A), commandData)
                command(keysOf(BANK_A), TokenContract.Command.Move())
                verifies()
            }
        }
    }

    @Test
    fun `On transfer advancing, the created timestamp must be after the consumed timestamp`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(BANK_B).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(BANK_B).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                input(TransferContract.ID, TRANSFER.copy(timestamp = Instant.MAX))
                output(TransferContract.ID, TRANSFER.acceptTransfer().copy(timestamp = Instant.MIN))
                command(keysOf(BANK_B), commandData)
                failsWith(commandData.CONTRACT_RULE_TIMESTAMP)
            }
        }
    }

    @Test
    fun `On transfer advancing, the advancing participant must sign the transaction`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(BANK_B).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(BANK_B).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                input(TransferContract.ID, TRANSFER)
                output(TransferContract.ID, TRANSFER.acceptTransfer())
                command(keysOf(TOKEN_ISSUING_ENTITY), commandData)
                failsWith(commandData.CONTRACT_RULE_SIGNERS)
            }
        }
    }
}
