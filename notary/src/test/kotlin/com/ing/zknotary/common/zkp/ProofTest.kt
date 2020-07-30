package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.transactions.ZKProverTransaction
import com.ing.zknotary.common.transactions.ZKProverTransactionFactory
import com.ing.zknotary.notary.transactions.createTestsState
import com.ing.zknotary.notary.transactions.moveTestsState
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.PedersenDigestService
import net.corda.core.crypto.sign
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Before
import org.junit.Test

class ProofTest {
    private val alice = TestIdentity.fresh("alice", Crypto.EDDSA_ED25519_SHA512)
    private val bob = TestIdentity.fresh("bob", Crypto.EDDSA_ED25519_SHA512)

    private val ledgerServices = MockServices(
        listOf("com.ing.zknotary.common.contracts"),
        alice
    )

    private lateinit var ptx: ZKProverTransaction
    private lateinit var sigAlice: ByteArray

    @Before
    fun setup() {
        ledgerServices.ledger {
            val wtx = moveTestsState(createTestsState(owner = alice), newOwner = bob)
            verifies()

            // Build a ZKProverTransaction
            ptx = ZKProverTransactionFactory.create(
                wtx.toLedgerTransaction(ledgerServices),
                componentGroupLeafDigestService = BLAKE2s256DigestService,
                nodeDigestService = PedersenDigestService
            )

            // Collect signatures
            sigAlice = alice.keyPair.private.sign(ptx.id.bytes).bytes
        }
    }

    @Test
    fun `Quick prove that ZKProverTransaction satisfies Zinc circuit logic`() {
        ledgerServices.ledger {
            val witness = MockWitness(ptx, listOf(sigAlice))
            val proof = MockProof(witness)
            proof.verify(MockInstance(ptx.id))
        }
    }
}
