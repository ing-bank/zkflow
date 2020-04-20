package com.ing.zknotary.notary.transactions

import com.ing.zknotary.common.serializer.VictorsZKInputSerializer
import com.ing.zknotary.common.zkp.ZincProverCLI
import com.ing.zknotary.common.zkp.ZincZKVerifierCLI
import net.corda.core.crypto.sign
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class VictorsSerializeProveVerifyTest {

    private val alice = TestIdentity.fresh("alice")
    private val bob = TestIdentity.fresh("bob")

    private val services = MockServices(
        listOf("com.ing.zknotary.common.contracts"),
        alice
    )

    @Test
    fun `Victor - prove and verify with valid tx is successful`() {
        services.ledger {
            val wtx = moveTestsState(createTestsState(owner = alice), newOwner = bob)
            verifies()

            val ltx = wtx.toLedgerTransaction(services)
            val sigAlice = alice.keyPair.sign(wtx.id).bytes

            // Check out JsonZKInputSerializer for reference
            val witness = VictorsZKInputSerializer(services).serializeWitness(ltx, listOf(sigAlice))
            val instance = VictorsZKInputSerializer(services).serializeInstance(wtx.id)

            val proof = ZincProverCLI(services).prove(witness, instance)

            // No assertions required: this throws an exception on verification failure
            ZincZKVerifierCLI(services).verify(proof, instance)
        }
    }
}

