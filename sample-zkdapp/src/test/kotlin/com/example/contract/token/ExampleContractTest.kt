package com.example.contract.token

import com.example.contract.token.commands.IssuePrivate
import com.example.contract.token.commands.MovePrivate
import com.example.contract.token.commands.RedeemPrivate
import com.example.contract.token.commands.SplitPrivate
import com.ing.zkflow.testing.dsl.zkLedger
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.jupiter.api.Test
import java.time.Instant

class ExampleContractTest {
    private val services = MockServices(listOf("com.example.contract"))

    private val ecb = TestIdentity.fresh("ECB").party
    private val alice = TestIdentity.fresh("Alice").party.anonymise()
    private val bob = TestIdentity.fresh("Bob").party.anonymise()

    private val alicesEuro = digitalEuro(1.00, issuer = ecb, holder = alice)

    @Test
    fun `Issue one EUR to Alice`() {
        services.zkLedger {
            transaction {
                output(ExampleTokenContract.ID, alicesEuro)
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
                output(ExampleTokenContract.ID, alicesEuro)
                command(listOf(ecb.owningKey), IssuePrivate())
                timeWindow(Instant.now())
                verifies()
            }
            transaction {
                input(issueTx.outRef<ExampleToken>(0).ref)
                command(listOf(alice.owningKey), MovePrivate())
                timeWindow(Instant.now())
                tweak {
                    output(ExampleTokenContract.ID, alicesEuro.withNewHolder(bob, 0.9))
                    `fails with`("Amounts of input and output must equal")
                }
                tweak {
                    output(ExampleTokenContract.ID, alicesEuro.withNewHolder(bob))
                    output(ExampleTokenContract.ID, alicesEuro.withNewHolder(bob))
                    `fails with`("There should be no additional 'public only' outputs")
                }
                output(ExampleTokenContract.ID, alicesEuro.withNewHolder(bob))
                verifies()
            }
        }
    }

    @Test
    fun `issue and split`() {
        services.zkLedger {
            val issueTx = transaction {
                output(ExampleTokenContract.ID, alicesEuro)
                command(listOf(ecb.owningKey), IssuePrivate())
                timeWindow(Instant.now())
                verifies()
            }
            val half = alicesEuro.copy(alicesEuro.amount.splitEvenly(2).first())

            transaction {
                input(issueTx.outRef<ExampleToken>(0).ref)
                output(ExampleTokenContract.ID, half) // change to self
                command(listOf(alice.owningKey), SplitPrivate())
                timeWindow(Instant.now())
                tweak {
                    output(ExampleTokenContract.ID, half.withNewHolder(bob, 0.1)) // amount total not conserved
                    `fails with`("Amounts of funds must be constant")
                }
                output(ExampleTokenContract.ID, half.withNewHolder(bob))
                verifies()
            }
        }
    }

    @Test
    fun `issue and redeem`() {
        services.zkLedger {
            val issueTx = transaction {
                output(ExampleTokenContract.ID, alicesEuro)
                command(listOf(ecb.owningKey), IssuePrivate())
                timeWindow(Instant.now())
                verifies()
            }
            val half = alicesEuro.copy(alicesEuro.amount.splitEvenly(2).first())

            transaction {
                input(issueTx.outRef<ExampleToken>(0).ref)
                command(listOf(alice.owningKey, ecb.owningKey), RedeemPrivate())
                timeWindow(Instant.now())
                tweak {
                    output(ExampleTokenContract.ID, half.withNewHolder(bob, 0.1))
                    `fails with`("There should be no additional 'public only' outputs")
                }
                verifies()
            }
        }
    }
}
