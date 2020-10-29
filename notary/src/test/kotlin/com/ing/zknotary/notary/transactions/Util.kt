package com.ing.zknotary.notary.transactions

import com.ing.zknotary.common.contracts.TestContract
import net.corda.core.contracts.StateAndRef
import net.corda.core.transactions.WireTransaction
import net.corda.testing.core.TestIdentity
import net.corda.testing.dsl.LedgerDSL
import net.corda.testing.dsl.TestLedgerDSLInterpreter
import net.corda.testing.dsl.TestTransactionDSLInterpreter
import java.util.Random

fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.createTestWireTransaction(
    owner: TestIdentity,
    value: Int = Random().nextInt(1000)
): WireTransaction {
    val createdState = TestContract.TestState(owner.party, value)
    val wtx = transaction {
        command(listOf(owner.publicKey), TestContract.Create())
        output(TestContract.PROGRAM_ID, "Alice's asset", createdState)
        verifies()
    }
    verifies()

    return wtx
}

fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.createTestsState(
    owner: TestIdentity,
    value: Int = Random().nextInt(1000)
): StateAndRef<TestContract.TestState> {
    val createdState = TestContract.TestState(owner.party, value)
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
