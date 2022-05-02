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
    @Test
    fun `issue and verify`() {
        val alice = TestIdentity.fresh("Alice").party.anonymise()
        val services = MockServices(listOf("com.example.contract"))

        services.zkLedger {
            val issuedTokenType = IssuedTokenType(alice, TokenType("test-token", 2))
            val createState = CBDCToken(Amount.fromDecimal(BigDecimal.ONE, issuedTokenType), alice)
            transaction {
                output(CBDCContract.ID, createState)
                command(listOf(alice.owningKey), CBDCContract.Issue())
                timeWindow(Instant.now())
                verifies()
            }
        }
    }

    @Test
    fun `create and move verify`() {
        val alice = TestIdentity.fresh("Alice").party.anonymise()
        val bob = TestIdentity.fresh("Bob").party.anonymise()
        val services = MockServices(listOf("com.example.contract"))

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
}
