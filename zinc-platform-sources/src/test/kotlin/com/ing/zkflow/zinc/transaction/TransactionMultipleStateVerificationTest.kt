package com.ing.zkflow.zinc.transaction

import com.ing.zkflow.contract.TestMultipleStateContract
import com.ing.zkflow.testing.dsl.VerificationMode
import com.ing.zkflow.testing.dsl.zkLedger
import com.ing.zkflow.testing.zkp.ZKNulls
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.jupiter.api.Test

/**
 * This is the multi-state version of `TransactionVerificationTest` in order to show that zinc can handle multiple state types.
 */
class TransactionMultipleStateVerificationTest {
    @Test
    fun `create and move verify`() {
        val alice = TestIdentity.fresh("Alice").party.anonymise()
        val bob = ZKNulls.NULL_ANONYMOUS_PARTY
        val services = MockServices(listOf("com.ing.zkflow.contract"))

        val createState1 = TestMultipleStateContract.TestState1(alice, value = 88)
        val createState2 = TestMultipleStateContract.TestState2(bob, value = 99)

        services.zkLedger {
            zkTransaction {
                input(TestMultipleStateContract.PROGRAM_ID, createState1)
                input(TestMultipleStateContract.PROGRAM_ID, createState2)
                output(TestMultipleStateContract.PROGRAM_ID, createState1.withNewOwner(bob).ownableState)
                output(TestMultipleStateContract.PROGRAM_ID, createState2.withNewOwner(alice).ownableState)
                command(listOf(alice.owningKey, bob.owningKey), TestMultipleStateContract.Move())
                verifies(VerificationMode.RUN)
            }
        }
    }
}
