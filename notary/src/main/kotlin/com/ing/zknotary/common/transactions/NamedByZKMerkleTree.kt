package com.ing.zknotary.common.transactions

import net.corda.core.KeepForDJVM

/**
 * Implemented by all transactions. This merkle root is an additional identifier to [NamedByHash.id].
 *
 */
@KeepForDJVM
interface NamedByZKMerkleTree {
    /**
     * A [ZKFullMerkleTree] that identifies this transaction.
     */
    val merkleTree: ZKFullMerkleTree
}
