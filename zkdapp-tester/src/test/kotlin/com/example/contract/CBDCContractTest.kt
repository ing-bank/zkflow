package com.example.contract

import com.example.token.cbdc.CBDCToken
import com.example.token.cbdc.IssuedTokenType
import com.example.token.cbdc.TokenType
import com.ing.zkflow.testing.dsl.zkLedger
import net.corda.core.contracts.Amount
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class CBDCContractTest {
    private val alice = TestIdentity.fresh("Alice").party.anonymise()
    private val services = MockServices(listOf("com.example.contract"))
    private val bob = TestIdentity.fresh("Bob").party.anonymise()

    @Test
    fun `issue`() {
        services.zkLedger {
            val issuedTokenType = IssuedTokenType(alice, TokenType("test-token", 2))
            val createState = CBDCToken(Amount.fromDecimal(BigDecimal.ONE, issuedTokenType), alice)
            transaction {
                output(CBDCContract.ID, createState)
                command(listOf(alice.owningKey), CBDCContract.IssuePrivate())
                timeWindow(Instant.now())
                verifies()
            }
        }
    }

    @Test
    fun `failed issue - wrong signer`() {
        services.zkLedger {
            val issuedTokenType = IssuedTokenType(alice, TokenType("test-token", 2))
            val createState = CBDCToken(Amount.fromDecimal(BigDecimal.ONE, issuedTokenType), alice)
            transaction {
                output(CBDCContract.ID, createState)
                command(listOf(bob.owningKey), CBDCContract.IssuePrivate())
                timeWindow(Instant.now())
                fails()
            }
        }
    }

    @Test
    fun `issue and move`() {
        services.zkLedger {
            val issuedTokenType = IssuedTokenType(alice, TokenType("test-token", 2))
            val createState = CBDCToken(Amount.fromDecimal(BigDecimal.ONE, issuedTokenType), alice)
            transaction {
                input(CBDCContract.ID, createState)
                output(CBDCContract.ID, createState.withNewHolder(bob))
                command(listOf(alice.owningKey, bob.owningKey), CBDCContract.Move())
                timeWindow(Instant.now())
                verifies()
            }
        }
    }

    @Test
    fun `issue and split`() {
        services.zkLedger {
            val issuedTokenType = IssuedTokenType(alice, TokenType("test-token", 2))

            val unit = CBDCToken(Amount.fromDecimal(BigDecimal.ONE, issuedTokenType), alice)
            val half = CBDCToken(Amount.fromDecimal(0.5.toBigDecimal(), issuedTokenType), alice)

            transaction {
                input(CBDCContract.ID, unit)
                output(CBDCContract.ID, half)
                output(CBDCContract.ID, half.withNewHolder(bob))
                command(listOf(alice.owningKey, bob.owningKey), CBDCContract.Split())
                timeWindow(Instant.now())
                verifies()
            }
        }
    }
}
