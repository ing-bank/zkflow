package com.ing.zkflow.common.contract

import com.ing.zkflow.testing.dsl.zkLedger
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.jupiter.api.Test

/**
 * This test confirms that relevant checks from [net.corda.core.internal.Verifier.verify] are implemented in the generated Zinc code
 */
class PlatformChecksContractTest {
    private val services = MockServices(listOf("com.ing.zkflow"))
    private val alice = TestIdentity.fresh("Alice").party.anonymise()
    private val bob = TestIdentity.fresh("Bobby").party.anonymise()
    private val aliceAsset = ZKTestState(alice)
    private val bobAsset = aliceAsset.copy(owner = bob)

    // checkNoNotaryChange()
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
}
