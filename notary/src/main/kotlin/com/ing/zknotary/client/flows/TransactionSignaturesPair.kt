package com.ing.zknotary.client.flows

import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import net.corda.core.crypto.TransactionSignature
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction

/**
 * We need these pairs to handle notary-only zk validation because in this case we need 2 different Merkle Trees and
 * 2 different sets of signatures for different Merkle Tree roots (normal tree and ZK-tree).
 * This will be redundant when/if we can use WireTransaction for both plaintext and ZKP forms.
 */
@CordaSerializable
data class TransactionsPair(val stx: SignedTransaction, val svtx: SignedZKVerifierTransaction)

@CordaSerializable
data class TransactionSignaturesPair(val sig: TransactionSignature, val zksig: TransactionSignature)
