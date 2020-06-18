package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.serializer.ZKJsonSerializationFactoryService
import com.ing.zknotary.common.serializer.ZincSerializationFactoryService
import com.ing.zknotary.notary.transactions.createTestsState
import com.ing.zknotary.notary.transactions.moveTestsState
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.sign
import net.corda.core.serialization.serialize
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.io.File
import java.nio.file.Paths
import java.security.PublicKey

class ZKMerkleTreeTest {
    private val alice = TestIdentity.fresh("alice", Crypto.EDDSA_ED25519_SHA512)
    private val bob = TestIdentity.fresh("bob", Crypto.EDDSA_ED25519_SHA512)

    private val ledgerServices = MockServices(
        listOf("com.ing.zknotary.common.contracts"),
        alice
    )

    @Test
    fun `can recalculate zkid based on serialized zkltx`() {
        ledgerServices.ledger {
            val wtx = moveTestsState(createTestsState(owner = alice), newOwner = bob)
            verifies()

            val ltx = wtx.toLedgerTransaction(ledgerServices)
            val serializationFactoryService = ZincSerializationFactoryService()

            val ptx = ZKProverTransactionFactory.create(
                ltx,
                serializationFactoryService,
                componentGroupLeafDigestService = BLAKE2s256DigestService,
                nodeDigestService = BLAKE2s256DigestService // Should become Pedersen hash when available
            )

            // Collect signatures
            val sigAlice = alice.keyPair.private.sign(ptx.id.bytes).bytes

            val json = ptx.serialize(serializationFactoryService.factory)
            println(String(json.bytes))

            val cwd = System.getProperty("user.dir")
            val circuitWd = Paths.get("$cwd/../prover/ZKMerkleTree").normalize().toString()
            File("$circuitWd/data/witness.json").writeText(String(json.bytes))
        }
    }

    data class MockWitness(
        val transaction: ZKProverTransaction,
        val signatures: List<ByteArray>
    )

    data class MockInstance(
        val zkId: SecureHash
    )

    // This is the logic that should be in the proving circuit
    class MockProof(
        private val witness: MockWitness,
        instance: MockInstance
    ) {
        init {
            verify(instance)
        }

        fun verify(instance: MockInstance) {
            // Do platform checks from TransactionVerifierServiceInternal.kt:44 that can't be done outside proof

            // build Merkle tree with serialized components and compare root with instance.zkId
            val tree = ZKMerkleTree(
                witness.transaction,
                serializationFactoryService = ZKJsonSerializationFactoryService(),
                componentGroupLeafDigestService = BLAKE2s256DigestService,
                nodeDigestService = BLAKE2s256DigestService // Should become Pedersen hash when available
            )

            // confirm tree.root == instance.zkId
            require(tree.root == instance.zkId)

            // Check all required signatures are present
            require(
                witness.transaction.commands.flatMap { it.signers }.all { signatureExists(it, instance.zkId.bytes) }
            )

            // TODO: Verify contract business logic
        }

        private fun signatureExists(publicKey: PublicKey, signedData: ByteArray) = witness.signatures.any { sig ->
            Crypto.doVerify(
                Crypto.EDDSA_ED25519_SHA512,
                publicKey,
                sig,
                signedData
            )
        }
    }
}
