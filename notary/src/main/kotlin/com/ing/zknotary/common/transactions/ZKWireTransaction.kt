package com.ing.zknotary.common.transactions

import net.corda.core.crypto.Algorithm
import net.corda.core.crypto.DefaultDigestServiceFactory
import net.corda.core.transactions.WireTransaction

class ZKWireTransaction(val wtx: WireTransaction) :
    NamedByAdditionalMerkleTree {
    /** This additional merkle root is represented by the root hash of a Merkle tree over the transaction components. */
    override val additionalMerkleTree: ZKWireTransactionMerkleTree by lazy {
        ZKWireTransactionMerkleTree(
            this,
            componentGroupLeafDigestService = DefaultDigestServiceFactory.getService(Algorithm.BLAKE2s256()),
            nodeDigestService = DefaultDigestServiceFactory.getService(Algorithm.BLAKE2s256())
        )
    }
}
