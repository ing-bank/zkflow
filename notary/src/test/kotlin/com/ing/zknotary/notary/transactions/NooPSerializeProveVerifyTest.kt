package com.ing.zknotary.notary.transactions

import com.ing.zknotary.common.serializer.NoopZKInputSerializer
import com.ing.zknotary.common.zkp.NoopZKProver
import com.ing.zknotary.common.zkp.NoopZKVerifier
import net.corda.core.crypto.sign
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class NooPSerializeProveVerifyTest {

    private val alice = TestIdentity.fresh("alice")
    private val bob = TestIdentity.fresh("bob")

    private val services = MockServices(
        listOf("com.ing.zknotary.common.contracts"),
        alice
    )

    @Test
    fun `Noop - prove and verify with valid tx is successful`() {
        services.ledger {
            val wtx = moveTestsState(createTestsState(owner = alice), newOwner = bob)
            verifies()

            val ltx = wtx.toLedgerTransaction(services)

            val sigAlice = alice.keyPair.sign(wtx.id).bytes

            val witness = NoopZKInputSerializer(MockServices()).serializeWitness(ltx, listOf(sigAlice))
            val instance = NoopZKInputSerializer(MockServices()).serializeInstance(wtx.id)

            val proof = NoopZKProver(MockServices()).prove(witness, instance)

            NoopZKVerifier(MockServices()).verify(proof, instance)
        }
    }
}

