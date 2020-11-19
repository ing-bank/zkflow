package com.ing.zknotary.common.transactions

import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.TransactionSignature
import net.corda.core.transactions.TransactionWithSignatures
import net.corda.core.utilities.toBase58String
import java.security.PublicKey

class SignedZKVerifierTransaction(val tx: ZKVerifierTransaction, override val sigs: List<TransactionSignature>) : TransactionWithSignatures {

    override val id: SecureHash
        get() = tx.id

    override val requiredSigningKeys: Set<PublicKey>
        get() = tx.signers.toSet()

    override fun getKeyDescriptions(keys: Set<PublicKey>): List<String> {
        return keys.map { it.toBase58String() }
    }
}
