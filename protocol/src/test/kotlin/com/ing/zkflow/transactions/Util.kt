package com.ing.zkflow.transactions

import com.ing.zkflow.testing.fixtures.contract.TestContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.OwnableState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.transactions.WireTransaction
import net.corda.testing.core.TestIdentity
import net.corda.testing.dsl.LedgerDSL
import net.corda.testing.dsl.TestLedgerDSLInterpreter
import net.corda.testing.dsl.TestTransactionDSLInterpreter
import net.corda.testing.internal.withTestSerializationEnvIfNotSet
import java.util.Random

fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.createIssuanceWtx(
    owner: TestIdentity,
    value: Int = Random().nextInt(1000),
    label: String = "${owner.name.organisation}'s asset"
): WireTransaction {
    val createdState = TestContract.TestState(owner.party.anonymise(), value)
    return withTestSerializationEnvIfNotSet {
        val wtx = transaction {
            command(listOf(owner.publicKey), TestContract.Create())
            output(TestContract.PROGRAM_ID, label, createdState)
            verifies()
        }
        verifies()

        wtx
    }
}

fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.createMoveWtx(
    stateLabel: String,
    newOwner: TestIdentity,
    referenceStateRef: StateRef?
): WireTransaction {
    return withTestSerializationEnvIfNotSet {
        val stateAndRef = retrieveOutputStateAndRef(OwnableState::class.java, stateLabel)
        createMoveWtx(stateAndRef, newOwner, referenceStateRef)
    }
}

fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.createMoveWtx(
    stateLabel: String,
    newOwner: TestIdentity,
    referenceLabel: String? = null
): WireTransaction {
    return withTestSerializationEnvIfNotSet {
        val stateAndRef = retrieveOutputStateAndRef(OwnableState::class.java, stateLabel)
        val referenceStateRef =
            if (referenceLabel != null) retrieveOutputStateAndRef(
                ContractState::class.java,
                referenceLabel
            ).ref else null

        createMoveWtx(stateAndRef, newOwner, referenceStateRef)
    }
}

fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.createMoveWtx(
    stateAndRef: StateAndRef<OwnableState>,
    newOwner: TestIdentity,
    referenceLabel: String? = null
): WireTransaction {
    return withTestSerializationEnvIfNotSet {
        val referenceStateRef =
            if (referenceLabel != null) retrieveOutputStateAndRef(
                ContractState::class.java,
                referenceLabel
            ).ref else null

        createMoveWtx(stateAndRef, newOwner, referenceStateRef)
    }
}

fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.createMoveWtx(
    stateAndRef: StateAndRef<OwnableState>,
    newOwner: TestIdentity,
    referenceStateRef: StateRef?
): WireTransaction {
    return withTestSerializationEnvIfNotSet {
        val state = stateAndRef.state.data

        transaction {
            input(stateAndRef.ref)
            output(TestContract.PROGRAM_ID, state.withNewOwner(newOwner.party).ownableState)
            command(listOf(state.owner.owningKey), TestContract.Move())
            if (referenceStateRef != null) {
                reference(referenceStateRef)
            }
            verifies()
        }
    }
}
