package com.example.contract

import com.ing.zkflow.testing.dsl.zkLedger
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.jupiter.api.Test
import java.time.Instant

class MockAssetContractTest {
    @Test
    fun `create and move verify`() {
        val alice = TestIdentity.fresh("Alice").party.anonymise()
        val bob = TestIdentity.fresh("Bob").party.anonymise()
        val services = MockServices(listOf("com.example.contract"))

        services.zkLedger {
            val createState = MockAssetContract.MockAsset(alice, value = 88)
            transaction {
                input(MockAssetContract.ID, createState)
                output(MockAssetContract.ID, createState.withNewOwner(bob).ownableState)
                command(listOf(alice.owningKey, bob.owningKey), MockAssetContract.Move())
                timeWindow(Instant.now())
                verifies()
            }
        }
    }
}
