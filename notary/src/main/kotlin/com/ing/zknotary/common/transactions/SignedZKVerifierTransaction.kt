package com.ing.zknotary.common.transactions

import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.TransactionSignature
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.TransactionWithSignatures
import net.corda.core.utilities.toBase58String
import java.security.PublicKey

@CordaSerializable
data class SignedZKVerifierTransaction(val tx: ZKVerifierTransaction, override val sigs: List<TransactionSignature> = emptyList()) : TransactionWithSignatures {

    override val id: SecureHash
        get() = tx.id

    override val requiredSigningKeys: Set<PublicKey>
        get() = tx.requiredSigningKeys

    override fun getKeyDescriptions(keys: Set<PublicKey>): List<String> {
        return keys.map { it.toBase58String() }
    }

    operator fun plus(sig: TransactionSignature) = copy(tx = tx, sigs = sigs + sig)

    operator fun plus(sigList: Collection<TransactionSignature>) = copy(tx = tx, sigs = sigs + sigList)
}
