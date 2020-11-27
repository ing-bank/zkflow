package com.ing.zknotary.common.transactions

import net.corda.core.KeepForDJVM
import net.corda.core.contracts.NamedByHash

/**
 * Implemented by all zkp transactions. This merkle root is an additional identifier to [NamedByHash.id].
 *
 */
@KeepForDJVM
interface NamedByZKMerkleTree : NamedByHash {
    /**
     * A [ZKFullMerkleTree] that identifies this transaction.
     */
    val merkleTree: TransactionMerkleTree
}
