package com.example.contract.cbdc

import com.example.contract.cbdc.commands.IssuePrivate
import com.example.contract.cbdc.commands.MovePrivate
import com.example.contract.cbdc.commands.RedeemPrivate
import com.example.contract.cbdc.commands.SplitPrivate
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
                timeWindow(Instant.now())
                tweak {
                    command(listOf(bob.owningKey), IssuePrivate()) // wrong signer
                    fails()
                }
                command(listOf(ecb.owningKey), IssuePrivate())
                verifies()
            }
        }
    }

    @Test
    fun `Issue to Alice and move to Bob`() {
        services.zkLedger {
            val issueTx = transaction {
                output(CBDCContract.ID, alicesEuro)
                command(listOf(ecb.owningKey), IssuePrivate())
                timeWindow(Instant.now())
                verifies()
            }
            transaction {
                input(issueTx.outRef<CBDCToken>(0).ref)
                command(listOf(alice.owningKey), MovePrivate())
                timeWindow(Instant.now())
                tweak {
                    output(CBDCContract.ID, alicesEuro.withNewHolder(bob, 0.9))
                    `fails with`("Amounts of input and output must equal")
                }
                tweak {
                    output(CBDCContract.ID, alicesEuro.withNewHolder(bob))
                    output(CBDCContract.ID, alicesEuro.withNewHolder(bob))
                    `fails with`("There should be no additional 'public only' outputs")
                }
                output(CBDCContract.ID, alicesEuro.withNewHolder(bob))
                verifies()
            }
        }
    }

    @Test
    fun `issue and split`() {
        services.zkLedger {
            val issueTx = transaction {
                output(CBDCContract.ID, alicesEuro)
                command(listOf(ecb.owningKey), IssuePrivate())
                timeWindow(Instant.now())
                verifies()
            }
            val half = alicesEuro.copy(alicesEuro.amount.splitEvenly(2).first())

            transaction {
                input(issueTx.outRef<CBDCToken>(0).ref)
                output(CBDCContract.ID, half) // change to self
                command(listOf(alice.owningKey), SplitPrivate())
                timeWindow(Instant.now())
                tweak {
                    output(CBDCContract.ID, half.withNewHolder(bob, 0.1)) // amount total not conserved
                    `fails with`("Amounts of funds must be constant")
                }
                output(CBDCContract.ID, half.withNewHolder(bob))
                verifies()
            }
        }
    }

    @Test
    fun `issue and redeem`() {
        services.zkLedger {
            val issueTx = transaction {
                output(CBDCContract.ID, alicesEuro)
                command(listOf(ecb.owningKey), IssuePrivate())
                timeWindow(Instant.now())
                verifies()
            }
            val half = alicesEuro.copy(alicesEuro.amount.splitEvenly(2).first())

            transaction {
                input(issueTx.outRef<CBDCToken>(0).ref)
                command(listOf(alice.owningKey, ecb.owningKey), RedeemPrivate())
                timeWindow(Instant.now())
                tweak {
                    output(CBDCContract.ID, half.withNewHolder(bob, 0.1))
                    `fails with`("There should be no additional 'public only' outputs")
                }
                verifies()
            }
        }
    }
}
