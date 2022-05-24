package com.example.contract

import com.example.token.cbdc.digitalEuro
import com.ing.zkflow.testing.dsl.zkLedger
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.jupiter.api.Test
import java.time.Instant

class CBDCContractTest {
    private val services = MockServices(listOf("com.example.contract"))

    private val ecb = TestIdentity.fresh("ECB").party
    private val alice = TestIdentity.fresh("Alice").party.anonymise()
    private val bob = TestIdentity.fresh("Bob").party.anonymise()

    private val alicesEuro = digitalEuro(1.00, issuer = ecb, holder = alice)

    @Test
    fun `Issue one EUR to Alice`() {
        services.zkLedger {
            transaction {
                output(CBDCContract.ID, alicesEuro)
                command(listOf(ecb.owningKey), CBDCContract.IssuePrivate())
                timeWindow(Instant.now())
                verifies()
            }
        }
    }

    @Test
    fun `Issue one EUR to Alice - wrong signer`() {
        services.zkLedger {
            transaction {
                output(CBDCContract.ID, alicesEuro)
                command(listOf(bob.owningKey), CBDCContract.IssuePrivate())
                timeWindow(Instant.now())
                fails()
            }
        }
    }

    @Test
    fun `Issue to Alice and move to Bob`() {
        services.zkLedger {
            transaction {
                input(CBDCContract.ID, alicesEuro)
                output(CBDCContract.ID, alicesEuro.withNewHolder(bob))
                command(listOf(alice.owningKey), CBDCContract.Move())
                timeWindow(Instant.now())
                verifies()
            }
        }
    }

    @Test
    fun `issue and split`() {
        services.zkLedger {
            val half = alicesEuro.copy(alicesEuro.amount.splitEvenly(2).first())

            transaction {
                input(CBDCContract.ID, alicesEuro)
                output(CBDCContract.ID, half)
                output(CBDCContract.ID, half.withNewHolder(bob))
                command(listOf(alice.owningKey), CBDCContract.Split())
                timeWindow(Instant.now())
                verifies()
            }
        }
    }
}
