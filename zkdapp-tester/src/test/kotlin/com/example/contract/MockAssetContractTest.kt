package com.example.contract

import com.ing.zkflow.testing.dsl.zkLedger
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.jupiter.api.Test
import java.time.Instant

class MockAssetContractTest {
    private val alice = TestIdentity.fresh("Alice").party.anonymise()
    private val bob = TestIdentity.fresh("Bob").party.anonymise()
    private val services = MockServices(listOf("com.example.contract"))

    @Test
    fun `create and move verify`() {
        services.zkLedger {
            val createState = MockAssetContract.MockAsset(alice, value = 88)
            transaction {
                input(MockAssetContract.ID, createState) // Creates a public UTXO on the ledger with a dummy command
                output(MockAssetContract.ID, createState.withNewOwner(bob).ownableState)
                command(listOf(alice.owningKey, bob.owningKey), MockAssetContract.MovePublicToPrivate())
                timeWindow(Instant.now())
                verifies()
            }
        }
    }

    @Test
    fun `failed create and move verify - moved amount don't match`() {
        services.zkLedger {
            val createState = MockAssetContract.MockAsset(alice, value = 88)
            val roguesState = MockAssetContract.MockAsset(bob, value = createState.value + 100)
            transaction {
                input(MockAssetContract.ID, createState) // Creates a public UTXO on the ledger with a dummy command
                output(MockAssetContract.ID, roguesState)
                command(listOf(alice.owningKey, bob.owningKey), MockAssetContract.MovePublicToPrivate())
                timeWindow(Instant.now())
                fails()
            }
        }
    }
}
