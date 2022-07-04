package com.ing.zkflow.integration.client.flows.upgrade

import com.ing.zkflow.client.flows.getUpgradeCommand
import com.ing.zkflow.testing.dsl.zkLedger
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.jupiter.api.Test

class StateUpgradeContractTest {
    private val services = MockServices(listOf("com.ing.zkflow"))
    private val alice = TestIdentity.fresh("Alice").party.anonymise()
    private val bob = TestIdentity.fresh("Bobby").party.anonymise()

    @Test
    fun `Additional upgrade check`() {
        services.zkLedger {
            transaction {
                val stateToUpgrade = MyStateV1(alice)
                val upgradeCommandInstance = getUpgradeCommand(MyStateV1::class, MyStateV2::class, isPrivate = false)

                input(MyContract.PROGRAM_ID, stateToUpgrade)
                output(MyContract.PROGRAM_ID, MyStateV2(stateToUpgrade))
                tweak {
                    command(listOf(bob.owningKey), upgradeCommandInstance)
                    `fails with`("Input owner must sign")
                }
                command(listOf(alice.owningKey), upgradeCommandInstance)
                verifies()
            }
        }
    }
}
