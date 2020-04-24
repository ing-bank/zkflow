package com.ing.zknotary.common.transactions

import net.corda.core.KeepForDJVM

/**
 * Implemented by all transactions. This merkle root is an additional identifier to [NamedByHash.id].
 *
 */
@KeepForDJVM
interface NamedByZKMerkleTree {
    /**
     * A [WireTransactionMerkleTree] that identifies this transaction.
     *
     * This identifier is an additional merkle root of this transaction.
     * This enables flexibility in using additional, potentially less trusted algorithms for calculating this root.
     */
    val merkleTree: ZKMerkleTree
}
