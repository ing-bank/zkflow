package com.ing.zknotary.notary.transactions

import com.ing.zknotary.common.contracts.TestContract
import net.corda.core.contracts.StateAndRef
import net.corda.core.transactions.WireTransaction
import net.corda.testing.core.TestIdentity
import net.corda.testing.dsl.LedgerDSL
import net.corda.testing.dsl.TestLedgerDSLInterpreter
import net.corda.testing.dsl.TestTransactionDSLInterpreter

fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.createTestsState(owner: TestIdentity): StateAndRef<TestContract.TestState> {
    val createdState = TestContract.TestState(owner.party)
    val wtx = unverifiedTransaction {
        command(listOf(owner.publicKey), TestContract.Create())
        output(TestContract.PROGRAM_ID, "Alice's asset", createdState)
    }

    return wtx.outRef(createdState)
}

fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.moveTestsState(
    input: StateAndRef<TestContract.TestState>,
    newOwner: TestIdentity
): WireTransaction {
    val wtx = transaction {
        input(input.ref)
        output(TestContract.PROGRAM_ID, input.state.data.withNewOwner(newOwner.party).ownableState)
        command(listOf(input.state.data.owner.owningKey), TestContract.Move())
        verifies()
    }

    return wtx
}
