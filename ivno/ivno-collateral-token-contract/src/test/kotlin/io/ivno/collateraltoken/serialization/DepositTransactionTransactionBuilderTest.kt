package io.ivno.collateraltoken.serialization

import com.ing.zknotary.testing.dsl.zkLedger
import com.ing.zknotary.testing.zkp.MockZKTransactionService
import io.ivno.collateraltoken.contract.ContractTest
import io.ivno.collateraltoken.contract.DepositContract
import io.ivno.collateraltoken.contract.IvnoTokenTypeContract
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DepositTransactionTransactionBuilderTest : ContractTest() {
    @Test
    fun `On deposit requesting, the transaction must include the Request command`() {
        // TODO: Once the real circuit for the command exists, remove the MockZKTransactionService param
        services.zkLedger(zkService = MockZKTransactionService(services)) {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                fails()
                command(keysOf(BANK_A), DepositContract.Request)
                verifies()
            }
        }
    }
}

