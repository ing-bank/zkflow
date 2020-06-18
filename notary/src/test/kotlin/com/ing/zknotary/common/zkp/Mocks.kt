package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.serializer.ZincSerializationFactoryService
import com.ing.zknotary.common.transactions.ZKMerkleTree
import com.ing.zknotary.common.transactions.ZKProverTransaction
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import java.security.PublicKey

data class MockWitness(
    override val transaction: ZKProverTransaction,
    override val signatures: List<ByteArray>
) : Witness

data class MockInstance(
    val zkId: SecureHash
)

// This is the logic that should be in the proving circuit
class MockProof(
    private val witness: MockWitness
) {
    fun verify(instance: MockInstance) {
        // TODO: Do platform checks from TransactionVerifierServiceInternal.kt:44 that can't be done outside proof

        // build Merkle tree with serialized components and compare root with instance.zkId
        val tree = ZKMerkleTree(
            witness.transaction,
            serializationFactoryService = ZincSerializationFactoryService(),
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
