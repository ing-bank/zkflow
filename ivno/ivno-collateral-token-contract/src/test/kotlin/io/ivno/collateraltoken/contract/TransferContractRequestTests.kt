package io.ivno.collateraltoken.contract

import com.ing.zknotary.common.zkp.ZincZKTransactionService
import com.ing.zknotary.testing.dsl.VerificationMode
import com.ing.zknotary.testing.dsl.zkLedger
import com.ing.zknotary.testing.zkp.MockZKTransactionService
import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
class TransferContractRequestTests : ContractTest() {
    private val log = loggerFor<TransferContractRequestTests>()

    @Test
    fun `On transfer requesting, the transaction must include the Request command`() {
        // services.zkLedger(zkService = MockZKTransactionService(services)) {
        services.zkLedger {
            val zkService = this.interpreter.zkService as ZincZKTransactionService
            val time = measureTime {
                zkService.setup(TransferContract.Request)
            }
            log.info("[setup] $time")
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(BANK_B).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(BANK_B).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(TransferContract.ID, TRANSFER)
                fails()
                command(keysOf(BANK_A), TransferContract.Request)
                verifies(VerificationMode.PROVE_AND_VERIFY)
            }
        }
    }

    @Test
    @Disabled
    fun `On transfer requesting, zero transfer states must be consumed`() {
        services.zkLedger {
            zkTransaction {
                input(TransferContract.ID, TRANSFER)
                output(TransferContract.ID, TRANSFER)
                command(keysOf(BANK_A), TransferContract.Request)
                failsWith(TransferContract.Request.CONTRACT_RULE_TRANSFER_INPUTS)
            }
        }
    }

    @Test
    @Disabled
    fun `On transfer requesting, only one transfer state must be created`() {
        services.zkLedger {
            zkTransaction {
                output(TransferContract.ID, TRANSFER)
                output(TransferContract.ID, TRANSFER)
                command(keysOf(BANK_A), TransferContract.Request)
                failsWith(TransferContract.Request.CONTRACT_RULE_TRANSFER_OUTPUTS)
            }
        }
    }

    @Test
    @Disabled
    fun `On transfer requesting, the sender and the receiver accounts must not be the same`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(BANK_B).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(BANK_B).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(
                    TransferContract.ID,
                    TRANSFER.copy(targetTokenHolder = BANK_A.party.anonymise(), targetTokenHolderAccountId = "12345678")
                )
                command(keysOf(BANK_A), TransferContract.Request)
                failsWith(TransferContract.Request.CONTRACT_RULE_PARTICIPANTS)
            }
        }
    }

    @Test
    @Disabled
    fun `On transfer requesting, the amount must be greater than zero`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(BANK_B).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(BANK_B).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(TransferContract.ID, TRANSFER.copy(amount = AMOUNT_OF_ZERO_IVNO_TOKEN_POINTER))
                command(keysOf(BANK_A), TransferContract.Request)
                failsWith(TransferContract.Request.CONTRACT_RULE_AMOUNT)
            }
        }
    }

    @Test
    @Disabled
    fun `On transfer requesting, the status must be REQUESTED`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(BANK_B).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(BANK_B).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(TransferContract.ID, TRANSFER.copy(status = TransferStatus.COMPLETED))
                command(keysOf(BANK_A), TransferContract.Request)
                failsWith(TransferContract.Request.CONTRACT_RULE_STATUS)
            }
        }
    }

    @Test
    @Disabled
    fun `On transfer requesting, the initiator must sign the transaction`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(BANK_B).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(BANK_B).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(TransferContract.ID, TRANSFER)
                command(keysOf(TOKEN_ISSUING_ENTITY), TransferContract.Request)
                failsWith(TransferContract.Request.CONTRACT_RULE_SIGNERS)
            }
        }
    }
}
