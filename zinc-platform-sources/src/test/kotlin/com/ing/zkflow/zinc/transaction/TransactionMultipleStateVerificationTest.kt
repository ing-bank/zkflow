package com.ing.zkflow.zinc.transaction

import com.ing.zkflow.testing.dsl.VerificationMode
import com.ing.zkflow.testing.dsl.zkLedger
import com.ing.zkflow.testing.fixtures.contract.TestMultipleStateContract
import com.ing.zkflow.testing.zkp.ZKNulls
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Tag("slow")
class TransactionMultipleStateVerificationTest {

    private val cordapps = listOf(
        "com.ing.zkflow.testing.fixtures.contract"
    )

    /**
     * This is the multi-state version of `TransactionVerificationTest` in order to show that zinc can handle multiple state types.
     */
    @ExperimentalTime
    @Test
    fun `create and move verify`() {
        val alice = TestIdentity.fresh("Alice").party.anonymise()
        val bob = ZKNulls.NULL_ANONYMOUS_PARTY
        val services = MockServices(cordapps)
        // services.zkLedger(zkService = MockZKTransactionService(services)) {
        services.zkLedger {
            val createState1 = TestMultipleStateContract.TestState1(alice, value = 88)
            val createState2 = TestMultipleStateContract.TestState2(alice, value = 99, list = listOf(42, 43))
            val createTx = zkTransaction {
                output(TestMultipleStateContract.PROGRAM_ID, createState1)
                output(TestMultipleStateContract.PROGRAM_ID, createState2)
                command(alice.owningKey, TestMultipleStateContract.Create())
                timeWindow(time = Instant.EPOCH)
                verifies(VerificationMode.RUN)
            }
            val utxo1 = createTx.outRef<TestMultipleStateContract.TestState1>(0)
            val utxo2 = createTx.outRef<TestMultipleStateContract.TestState2>(1)
            val moveState1 = TestMultipleStateContract.TestState1(bob, value = createState1.value)
            val moveState2 = TestMultipleStateContract.TestState2(bob, value = createState2.value, list = createState2.list)
            zkTransaction {
                input(utxo1.ref)
                input(utxo2.ref)
                output(TestMultipleStateContract.PROGRAM_ID, moveState1)
                output(TestMultipleStateContract.PROGRAM_ID, moveState2)
                timeWindow(time = Instant.EPOCH)
                command(listOf(alice.owningKey, bob.owningKey), TestMultipleStateContract.Move())
                verifies(VerificationMode.RUN)
            }
        }
    }
}
