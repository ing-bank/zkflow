package com.ing.zkflow.common.contract

import com.ing.zkflow.testing.dsl.zkLedger
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.ContractState
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * This test confirms that relevant checks from [net.corda.core.internal.Verifier.verify] are implemented in the generated Zinc code
 */
class PlatformChecksContractTest {
    private val services = MockServices(listOf("com.ing.zkflow"))
    private val alice = TestIdentity.fresh("Alice").party.anonymise()
    private val bob = TestIdentity.fresh("Bobby").party.anonymise()
    private val aliceAsset = ZKTestState(alice)
    private val bobAsset = aliceAsset.copy(owner = bob)

    /**
     * [net.corda.core.internal.Verifier.checkNoNotaryChange]
     */
    @Test
    fun `checkNoNotaryChange checks implemented`() {
        services.zkLedger {
            transaction {
                val wrongNotary = TestIdentity.fresh("WrongNotary").party
                input(LocalZKContract.PROGRAM_ID, aliceAsset)
                output(LocalZKContract.PROGRAM_ID, null, wrongNotary, null, AlwaysAcceptAttachmentConstraint, bobAsset)
                command(listOf(alice.owningKey, bob.owningKey), LocalZKContract.MoveAnyToPrivate())
                `fails with`("Found unexpected notary change in transaction")
            }
        }
    }

    /**
     * [net.corda.core.internal.Verifier.checkEncumbrancesValid]
     */
    @Test
    fun `checkEncumbrancesValid checks for inputs implemented`() {
        val notary = TestIdentity.fresh("ledger notary").party
        services.zkLedger(notary = notary) {
            transaction {
                output(LocalZKContract.PROGRAM_ID, "Encumbered State 1", 1, aliceAsset)
                output(LocalZKContract.PROGRAM_ID, "Encumbering State 1", SomeOtherZKState())
                command(listOf(alice.owningKey), LocalZKContract.CreatePrivateEncumbered())
                verifies()
            }
            transaction {
                output(LocalZKContract.PROGRAM_ID, "Encumbered State 2", 1, aliceAsset)
                output(LocalZKContract.PROGRAM_ID, "Encumbering State 2", SomeOtherZKState())
                command(listOf(alice.owningKey), LocalZKContract.CreatePrivateEncumbered())
                verifies()
            }
            transaction {
                timeWindow(Instant.now())
                output(LocalZKContract.PROGRAM_ID, retrieveOutputStateAndRef(ContractState::class.java, "Encumbered State 1").state.data)
                command(listOf(alice.owningKey, bob.owningKey), LocalZKContract.MoveFullyPrivate())
                input("Encumbered State 1")
                `fails with`("Missing required encumbrance in inputs")
            }
            transaction {
                output(LocalZKContract.PROGRAM_ID, retrieveOutputStateAndRef(ContractState::class.java, "Encumbered State 1").state.data)
                command(listOf(alice.owningKey), LocalZKContract.UnencumberPrivate())
                input("Encumbered State 1")
                tweak {
                    input("Encumbering State 2")
                    `fails with`("Missing required encumbrance in inputs")
                }
                input("Encumbering State 1")
                verifies()
            }
        }
    }
}
