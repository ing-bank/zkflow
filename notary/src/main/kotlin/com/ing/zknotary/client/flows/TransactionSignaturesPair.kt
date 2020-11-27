package com.ing.zknotary.client.flows

import net.corda.core.crypto.TransactionSignature
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction

/**
 * We need these pairs to handle notary-only zk validation because in this case we need 2 different Merkle Trees and
 * 2 different sets of signatures for different Merkle Tree roots (normal tree and ZK-tree)
 */
@CordaSerializable
data class TransactionWithZKSignatures(val stx: SignedTransaction, val zksigs: List<TransactionSignature>)

@CordaSerializable
data class TransactionSignaturesPair(val sig: TransactionSignature, val zksig: TransactionSignature)
